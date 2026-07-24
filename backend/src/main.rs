use axum::{
    extract::{
        ws::{Message, WebSocket, WebSocketUpgrade},
        Path, State,
    },
    response::IntoResponse,
    routing::get,
    Router,
};
use futures_util::{sink::SinkExt, stream::StreamExt};
use std::{
    collections::HashMap,
    net::SocketAddr,
    sync::{Arc, Mutex},
};
use tokio::sync::mpsc;
use tracing::{info, warn};
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

// Message structure to relay: We can relay any text message directly.
// The client will send and receive JSON strings.
type Tx = mpsc::UnboundedSender<Message>;

struct Room {
    clients: HashMap<usize, Tx>,
    next_client_id: usize,
}

impl Room {
    fn new() -> Self {
        Self {
            clients: HashMap::new(),
            next_client_id: 0,
        }
    }
}

type ChatState = Arc<Mutex<HashMap<String, Room>>>;

#[tokio::main]
async fn main() {
    // Initialize tracing logger
    tracing_subscriber::registry()
        .with(tracing_subscriber::EnvFilter::new(
            std::env::var("RUST_LOG").unwrap_or_else(|_| "info".into()),
        ))
        .with(tracing_subscriber::fmt::layer())
        .init();

    let state = ChatState::default();

    let app = Router::new()
        .route("/ws/:room_id", get(ws_handler))
        .route("/health", get(health_check))
        .with_state(state);

    // Bind to 0.0.0.0 to allow connections from external devices (like the Android app)
    let addr = SocketAddr::from(([0, 0, 0, 0], 8080));
    info!("Secure room chat backend running on http://{}", addr);

    let listener = tokio::net::TcpListener::bind(&addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

async fn health_check() -> &'static str {
    "OK"
}

async fn ws_handler(
    ws: WebSocketUpgrade,
    Path(room_id): Path<String>,
    State(state): State<ChatState>,
) -> impl IntoResponse {
    ws.on_upgrade(move |socket| handle_socket(socket, room_id, state))
}

async fn handle_socket(socket: WebSocket, room_id: String, state: ChatState) {
    let (mut sender, mut receiver) = socket.split();

    // Create a channel for this client
    let (tx, mut rx) = mpsc::unbounded_channel::<Message>();

    // Register the client in the room
    let client_id = {
        let mut rooms = state.lock().unwrap();
        let room = rooms.entry(room_id.clone()).or_insert_with(Room::new);
        let id = room.next_client_id;
        room.next_client_id += 1;
        room.clients.insert(id, tx);
        info!("Client {} joined room {}", id, room_id);
        id
    };

    // Task 1: Forward messages from the channel rx to the WebSocket sender
    let mut send_task = tokio::spawn(async move {
        while let Some(msg) = rx.recv().await {
            if sender.send(msg).await.is_err() {
                break;
            }
        }
    });

    // Task 2: Listen for messages from the WebSocket receiver and broadcast them
    let room_id_clone = room_id.clone();
    let state_clone = state.clone();
    let mut recv_task = tokio::spawn(async move {
        while let Some(Ok(msg)) = receiver.next().await {
            // Only broadcast text or binary messages
            if matches!(msg, Message::Text(_) | Message::Binary(_)) {
                broadcast_message(&room_id_clone, client_id, msg, &state_clone);
            }
        }
    });

    // Wait for either task to complete (meaning connection closed or errored)
    tokio::select! {
        _ = (&mut send_task) => recv_task.abort(),
        _ = (&mut recv_task) => send_task.abort(),
    };

    // Cleanup: Remove the client from the room when they disconnect
    {
        let mut rooms = state.lock().unwrap();
        if let Some(room) = rooms.get_mut(&room_id) {
            room.clients.remove(&client_id);
            info!("Client {} left room {}", client_id, room_id);
            // If the room is empty, remove it completely from memory (transient state)
            if room.clients.is_empty() {
                rooms.remove(&room_id);
                info!("Room {} is empty. Removed room from memory.", room_id);
            }
        }
    }
}

fn broadcast_message(room_id: &str, sender_id: usize, msg: Message, state: &ChatState) {
    let rooms = state.lock().unwrap();
    if let Some(room) = rooms.get(room_id) {
        for (&client_id, tx) in &room.clients {
            // Relay to all clients in the room except the sender
            if client_id != sender_id {
                let _ = tx.send(msg.clone());
            }
        }
    }
}
