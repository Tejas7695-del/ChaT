# Deploying to Google Cloud VM (e2-micro) — 24/7 Free Hosting

This guide outlines how to set up, compile, and run your Rust WebSocket server on Google Cloud’s **Always Free** `e2-micro` virtual machine.

---

## Step 1: Create your Free VM in Google Cloud

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Navigate to **Compute Engine** > **VM Instances** and click **Create Instance**.
3. **Configure the VM**:
   - **Name**: `secure-chat-backend`
   - **Region**: Choose one of the Always Free eligible regions:
     - `us-west1` (Oregon)
     - `us-central1` (Iowa)
     - `us-east1` (South Carolina)
   - **Machine Configuration**:
     - **Series**: `E2`
     - **Machine type**: `e2-micro` (2 vCPUs, 1 GB RAM)
   - **Boot Disk**:
     - Operating System: `Ubuntu`
     - Version: `Ubuntu 22.04 LTS` or `24.04 LTS`
     - Size: 10 GB to 30 GB (Standard persistent disk)
   - **Firewall**:
     - Check **Allow HTTP traffic**
     - Check **Allow HTTPS traffic**
4. Click **Create**.

---

## Step 2: Configure the Firewall for WebSockets

Our server runs on port `8080`. We need to open this port to allow connections from the Android app:

1. In the GCP Console, search for **VPC network** > **Firewall**.
2. Click **Create Firewall Rule**.
3. Set the following details:
   - **Name**: `allow-chat-backend`
   - **Targets**: `All instances in the network`
   - **Source IPv4 ranges**: `0.0.0.0/0` (Allows connection from any IP)
   - **Protocols and ports**: Under *Specified protocols and ports*, check `TCP` and enter `8080`.
4. Click **Create**.

---

## Step 3: Connect to the VM and Install Rust

1. In the Compute Engine VM list, click the **SSH** button next to your instance.
2. Once connected, update the system:
   ```bash
   sudo apt update && sudo apt upgrade -y
   sudo apt install build-essential pkg-config libssl-dev -y
   ```
3. **Configure Swap Space** (CRITICAL):
   Because the VM has only 1 GB of RAM, compiling Rust directly on it might run out of memory. Enabling 2 GB of swap space prevents this:
   ```bash
   sudo fallocate -l 2G /swapfile
   sudo chmod 600 /swapfile
   sudo mkswap /swapfile
   sudo swapon /swapfile
   echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
   ```
4. **Install Rust**:
   ```bash
   curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
   # Press 1 and Enter for default installation
   source "$HOME/.cargo/env"
   ```

---

## Step 4: Transfer Code and Compile

1. On the VM, create the folder and copy/paste your `Cargo.toml` and `src/main.rs`:
   ```bash
   mkdir -p secure-room-chat/src
   nano secure-room-chat/Cargo.toml  # Paste Cargo.toml contents and save (Ctrl+O, Enter, Ctrl+X)
   nano secure-room-chat/src/main.rs # Paste main.rs contents and save
   ```
2. Build the project in release mode:
   ```bash
   cd secure-room-chat
   cargo build --release
   ```
   The compiled executable will be located at `target/release/secure-room-chat-backend`.

---

## Step 5: Run 24/7 as a Background Service

To ensure the server runs continuously and restarts automatically if the VM reboots:

1. Create a `systemd` service file:
   ```bash
   sudo nano /etc/systemd/system/secure-chat.service
   ```
2. Paste the following configuration (replace `YOUR_USERNAME` with your active SSH username, visible in the console command prompt):
   ```ini
   [Unit]
   Description=Secure Room Chat Backend Server
   After=network.target

   [Service]
   Type=simple
   User=YOUR_USERNAME
   WorkingDirectory=/home/YOUR_USERNAME/secure-room-chat
   ExecStart=/home/YOUR_USERNAME/secure-room-chat/target/release/secure-room-chat-backend
   Restart=always
   RestartSec=5
   Environment=RUST_LOG=info

   [Install]
   WantedBy=multi-user.target
   ```
3. Save and close. Then start the service:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl start secure-chat
   sudo systemctl enable secure-chat
   ```
4. Verify the server is running properly:
   ```bash
   sudo systemctl status secure-chat
   ```
   You should see `active (running)`.

---

## Step 6: Connecting your Android App

When building the Android app, set the WebSocket URL to:
`ws://[YOUR_VM_EXTERNAL_IP]:8080/ws/`

*(To make it highly secure, you can configure an SSL certificate using Nginx and Let's Encrypt later, changing the URL to `wss://yourdomain.com/ws/`).*
