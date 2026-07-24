import asyncio
import json
import logging
from websockets.server import serve

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(message)s")

# In-memory map: room_id -> set of websocket connections
ROOMS = {}

async def handler(websocket):
    # Retrieve connection path
    try:
        path = websocket.request.path
    except AttributeError:
        path = getattr(websocket, 'path', '/')

    parts = [p for p in path.split('/') if p]

    room_id = "default"
    if parts:
        room_id = parts[-1]
        if room_id == "ws" and len(parts) > 1:
            room_id = parts[-2]

    if room_id not in ROOMS:
        ROOMS[room_id] = set()

    ROOMS[room_id].add(websocket)
    logging.info(f"Client connected to room: '{room_id}' (Active peers: {len(ROOMS[room_id])})")

    try:
        async for message in websocket:
            # Broadcast incoming message to all other peers in the room
            peers = ROOMS[room_id].copy()
            for peer in peers:
                if peer != websocket:
                    try:
                        await peer.send(message)
                    except Exception:
                        ROOMS[room_id].discard(peer)
    except Exception as e:
        logging.info(f"Connection ended for room '{room_id}': {e}")
    finally:
        if room_id in ROOMS:
            ROOMS[room_id].discard(websocket)
            logging.info(f"Client left room '{room_id}' (Remaining: {len(ROOMS[room_id])})")
            if not ROOMS[room_id]:
                del ROOMS[room_id]
                logging.info(f"Room '{room_id}' closed and removed from memory.")

async def main():
    # Listen on 0.0.0.0:8080
    async with serve(handler, "0.0.0.0", 8080, ping_interval=20, ping_timeout=20):
        logging.info("ChaT WebSocket backend running on ws://0.0.0.0:8080")
        await asyncio.Future()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nServer stopped.")
