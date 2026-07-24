// Client-side Cryptography using Web Crypto API (AES-256-GCM)

class CryptoHelper {
    static async generateSecretKey() {
        const key = await window.crypto.subtle.generateKey(
            { name: "AES-GCM", length: 256 },
            true,
            ["encrypt", "decrypt"]
        );
        const exported = await window.crypto.subtle.exportKey("raw", key);
        return this.bufferToBase64(exported);
    }

    static generate6DigitCode() {
        const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Removed similar looking characters
        let result = "";
        const bytes = new Uint8Array(6);
        window.crypto.getRandomValues(bytes);
        for (let i = 0; i < 6; i++) {
            result += chars[bytes[i] % chars.length];
        }
        return result;
    }

    static async deriveFromCode(code) {
        const encoder = new TextEncoder();
        const data = encoder.encode(code);
        const hashBuffer = await window.crypto.subtle.digest('SHA-256', data);
        
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        const roomId = hashArray.slice(0, 3).map(b => b.toString(16).padStart(2, '0')).join('');
        const secretKey = this.bufferToBase64(hashBuffer);
        
        return { roomId, secretKey };
    }

    static async encrypt(plaintext, base64Key) {
        const keyBuffer = this.base64ToBuffer(base64Key);
        const cryptoKey = await window.crypto.subtle.importKey(
            "raw", keyBuffer, { name: "AES-GCM" }, false, ["encrypt"]
        );

        const iv = window.crypto.getRandomValues(new Uint8Array(12));
        const encoded = new TextEncoder().encode(plaintext);

        const ciphertext = await window.crypto.subtle.encrypt(
            { name: "AES-GCM", iv: iv }, cryptoKey, encoded
        );

        const ivBase64 = this.bufferToBase64(iv);
        const ciphertextBase64 = this.bufferToBase64(ciphertext);

        return `${ivBase64}:${ciphertextBase64}`;
    }

    static async decrypt(encryptedData, base64Key) {
        try {
            const parts = encryptedData.split(":");
            if (parts.length !== 2) return "[Invalid Payload]";

            const iv = new Uint8Array(this.base64ToBuffer(parts[0]));
            const ciphertext = this.base64ToBuffer(parts[1]);
            const keyBuffer = this.base64ToBuffer(base64Key);

            const cryptoKey = await window.crypto.subtle.importKey(
                "raw", keyBuffer, { name: "AES-GCM" }, false, ["decrypt"]
            );

            const decrypted = await window.crypto.subtle.decrypt(
                { name: "AES-GCM", iv: iv }, cryptoKey, ciphertext
            );

            return new TextDecoder().decode(decrypted);
        } catch (e) {
            console.error(e);
            return "[Decryption Failed]";
        }
    }

    static bufferToBase64(buffer) {
        const bytes = new Uint8Array(buffer);
        let binary = "";
        for (let i = 0; i < bytes.byteLength; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    }

    static base64ToBuffer(base64) {
        let str = base64.replace(/-/g, '+').replace(/_/g, '/');
        while (str.length % 4) str += '=';
        const binary = atob(str);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes.buffer;
    }
}

// App Logic
document.addEventListener("DOMContentLoaded", () => {
    const views = document.querySelectorAll(".view");
    const serverUrlInput = document.getElementById("server-url");
    const btnToggleSettings = document.getElementById("btn-toggle-settings");
    const settingsPanel = document.getElementById("settings-panel");
    const nicknameInput = document.getElementById("nickname-input");
    const nicknameError = document.getElementById("nickname-error");
    const btnCreateRoom = document.getElementById("btn-create-room");
    const btnShowJoin = document.getElementById("btn-show-join");
    const btnConnectRoom = document.getElementById("btn-connect-room");
    const createdAddressInput = document.getElementById("created-address");
    const btnCopyAddress = document.getElementById("btn-copy-address");
    const btnEnterCreatedRoom = document.getElementById("btn-enter-created-room");
    const joinAddressInput = document.getElementById("join-address-input");
    const joinError = document.getElementById("join-error");
    const chatRoomId = document.getElementById("chat-room-id");
    const statusDot = document.getElementById("status-dot");
    const statusText = document.getElementById("status-text");
    const chatMessages = document.getElementById("chat-messages");
    const chatInput = document.getElementById("chat-input");
    const btnSendMsg = document.getElementById("btn-send-msg");
    const btnLeaveChat = document.getElementById("btn-leave-chat");
    const btnTerminateChat = document.getElementById("btn-terminate-chat");
    const btnAttach = document.getElementById("btn-attach");
    const fileInput = document.getElementById("file-input");
    const lightbox = document.getElementById("lightbox");
    const lightboxContent = document.getElementById("lightbox-content");
    const lightboxClose = document.getElementById("lightbox-close");

    let socket = null;
    let currentRoomId = null;
    let currentSecretKey = null;
    let currentRoomCode = null;
    let myNickname = "";
    let isAdmin = false;

    function showView(viewId) {
        views.forEach(v => {
            if (v.id === viewId) {
                v.classList.remove("hidden");
                v.classList.add("active");
            } else {
                v.classList.add("hidden");
                v.classList.remove("active");
            }
        });
    }

    document.querySelectorAll(".btn-back").forEach(btn => {
        btn.addEventListener("click", () => {
            const target = btn.getAttribute("data-target") || "view-welcome";
            if (socket) {
                socket.close();
                socket = null;
            }
            showView(target);
        });
    });

    btnToggleSettings.addEventListener("click", () => {
        settingsPanel.classList.toggle("hidden");
    });

    btnCreateRoom.addEventListener("click", async () => {
        let alias = nicknameInput.value.trim();
        if (!alias) {
            alias = "Anon-" + Math.floor(1000 + Math.random() * 9000);
        }
        myNickname = alias;
        isAdmin = true;

        const code = CryptoHelper.generate6DigitCode();
        const derived = await CryptoHelper.deriveFromCode(code);

        currentRoomId = derived.roomId;
        currentSecretKey = derived.secretKey;
        currentRoomCode = code;
        createdAddressInput.value = code;

        const qrContainer = document.getElementById("qrcode");
        qrContainer.innerHTML = "";
        new QRCode(qrContainer, {
            text: code,
            width: 160,
            height: 160,
            colorDark : "#0A0E17",
            colorLight : "#ffffff"
        });

        showView("view-create");
    });

    btnCopyAddress.addEventListener("click", () => {
        navigator.clipboard.writeText(createdAddressInput.value);
        btnCopyAddress.innerText = "✓ Copied!";
        setTimeout(() => btnCopyAddress.innerText = "📋 Copy", 2000);
    });

    btnEnterCreatedRoom.addEventListener("click", () => {
        connectToRoom(currentRoomId, currentSecretKey, currentRoomCode);
    });

    btnShowJoin.addEventListener("click", () => {
        let alias = nicknameInput.value.trim();
        if (!alias) {
            alias = "Anon-" + Math.floor(1000 + Math.random() * 9000);
        }
        myNickname = alias;
        isAdmin = false;

        joinAddressInput.value = "";
        joinError.classList.add("hidden");
        showView("view-join");
    });

    btnConnectRoom.addEventListener("click", async () => {
        const code = joinAddressInput.value.trim().toUpperCase();
        if (code.length === 6) {
            joinError.classList.add("hidden");
            const derived = await CryptoHelper.deriveFromCode(code);
            connectToRoom(derived.roomId, derived.secretKey, code);
        } else {
            joinError.innerText = "Invalid Room Code. It must be exactly 6 characters.";
            joinError.classList.remove("hidden");
        }
    });

    function connectToRoom(roomId, secretKey, code) {
        currentRoomId = roomId;
        currentSecretKey = secretKey;
        currentRoomCode = code;
        chatRoomId.innerText = `Code: ${code}`;
        chatMessages.innerHTML = "";

        if (isAdmin) {
            btnTerminateChat.classList.remove("hidden");
        } else {
            btnTerminateChat.classList.add("hidden");
        }

        showView("view-chat");

        let baseUrl = serverUrlInput.value.trim();
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.slice(0, -1);
        const fullWsUrl = `${baseUrl}/${roomId}`;

        updateStatus("connecting", "Connecting...");

        if (socket) {
            socket.close();
        }

        try {
            socket = new WebSocket(fullWsUrl);

            socket.onopen = () => {
                updateStatus("connected", "Encrypted Tunnel Active");
            };

            socket.onmessage = async (event) => {
                try {
                    const data = JSON.parse(event.data);
                    const decryptedData = await CryptoHelper.decrypt(data.payload, currentSecretKey);
                    
                    if (data.msgType === "control") {
                        try {
                            const controlData = JSON.parse(decryptedData);
                            if (controlData.action === "terminate") {
                                alert("The Admin has terminated this chat room.");
                                if (socket) socket.close();
                                showView("view-welcome");
                            }
                        } catch (e) {}
                    } else if (data.msgType === "image" || data.msgType === "video") {
                        appendMediaMessage(data.sender, decryptedData, data.msgType, false);
                    } else {
                        appendTextMessage(data.sender, decryptedData, false);
                    }
                } catch (e) {
                    console.error("Message parse error", e);
                }
            };

            socket.onclose = () => {
                updateStatus("disconnected", "Disconnected");
            };

            socket.onerror = (err) => {
                updateStatus("disconnected", "Connection Error");
            };
        } catch (e) {
            updateStatus("disconnected", "Invalid Backend URL");
        }
    }

    function updateStatus(stateClass, text) {
        statusDot.className = `dot ${stateClass}`;
        statusText.innerText = text;
    }

    // Render Text Message
    function appendTextMessage(sender, text, isSelf) {
        const wrapper = document.createElement("div");
        wrapper.className = `msg-wrapper ${isSelf ? 'self' : 'other'}`;

        if (!isSelf) {
            const senderEl = document.createElement("span");
            senderEl.className = "msg-sender";
            senderEl.innerText = sender;
            wrapper.appendChild(senderEl);
        }

        const bubble = document.createElement("div");
        bubble.className = "msg-bubble";
        bubble.innerText = text;

        wrapper.appendChild(bubble);
        chatMessages.appendChild(wrapper);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    // Render Media Message (Image or Video)
    function appendMediaMessage(sender, mediaUrl, msgType, isSelf) {
        const wrapper = document.createElement("div");
        wrapper.className = `msg-wrapper ${isSelf ? 'self' : 'other'}`;

        if (!isSelf) {
            const senderEl = document.createElement("span");
            senderEl.className = "msg-sender";
            senderEl.innerText = sender;
            wrapper.appendChild(senderEl);
        }

        const bubble = document.createElement("div");
        bubble.className = "msg-bubble";

        const mediaContainer = document.createElement("div");
        mediaContainer.className = "media-container";

        if (msgType === "image") {
            const img = document.createElement("img");
            img.src = mediaUrl;
            img.className = "media-img";
            img.alt = "Encrypted image";
            img.addEventListener("click", () => openLightbox(mediaUrl, "image"));
            mediaContainer.appendChild(img);
        } else if (msgType === "video") {
            const video = document.createElement("video");
            video.src = mediaUrl;
            video.className = "media-video";
            video.controls = true;
            mediaContainer.appendChild(video);
        }

        bubble.appendChild(mediaContainer);
        wrapper.appendChild(bubble);
        chatMessages.appendChild(wrapper);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    // Lightbox Modal
    function openLightbox(url, type) {
        lightboxContent.innerHTML = "";
        if (type === "image") {
            const img = document.createElement("img");
            img.src = url;
            lightboxContent.appendChild(img);
        } else if (type === "video") {
            const video = document.createElement("video");
            video.src = url;
            video.controls = true;
            video.autoplay = true;
            lightboxContent.appendChild(video);
        }
        lightbox.classList.remove("hidden");
    }

    lightboxClose.addEventListener("click", () => {
        lightbox.classList.add("hidden");
    });
    lightbox.addEventListener("click", (e) => {
        if (e.target === lightbox) lightbox.classList.add("hidden");
    });

    // Send Text Message
    async function handleSend() {
        const text = chatInput.value.trim();
        if (!text || !socket || socket.readyState !== WebSocket.OPEN) return;

        chatInput.value = "";
        const encryptedPayload = await CryptoHelper.encrypt(text, currentSecretKey);

        const msgObj = {
            sender: myNickname,
            msgType: "text",
            payload: encryptedPayload
        };

        socket.send(JSON.stringify(msgObj));
        appendTextMessage(myNickname, text, true);
    }

    // File Attachment Handling (Images & Videos)
    btnAttach.addEventListener("click", () => {
        fileInput.click();
    });

    fileInput.addEventListener("change", async (e) => {
        const file = e.target.files[0];
        if (!file || !socket || socket.readyState !== WebSocket.OPEN) return;

        // Check if file is image or video
        const isImage = file.type.startsWith("image/");
        const isVideo = file.type.startsWith("video/");
        if (!isImage && !isVideo) {
            alert("Please select an image or video file.");
            return;
        }

        // Check size limit (max 15MB for in-browser real-time WebSocket)
        if (file.size > 15 * 1024 * 1024) {
            alert("File is too large. Please select a file smaller than 15MB.");
            return;
        }

        const reader = new FileReader();
        reader.onload = async (event) => {
            const dataUrl = event.target.result;
            const msgType = isImage ? "image" : "video";

            const encryptedPayload = await CryptoHelper.encrypt(dataUrl, currentSecretKey);

            const msgObj = {
                sender: myNickname,
                msgType: msgType,
                payload: encryptedPayload
            };

            socket.send(JSON.stringify(msgObj));
            appendMediaMessage(myNickname, dataUrl, msgType, true);
        };
        reader.readAsDataURL(file);

        // Reset file input
        fileInput.value = "";
    });

    btnSendMsg.addEventListener("click", handleSend);
    chatInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter") handleSend();
    });

    btnLeaveChat.addEventListener("click", () => {
        if (socket) socket.close();
        showView("view-welcome");
    });

    btnTerminateChat.addEventListener("click", async () => {
        if (!socket || socket.readyState !== WebSocket.OPEN) return;
        if (confirm("Are you sure you want to terminate this room for everyone?")) {
            const encryptedPayload = await CryptoHelper.encrypt(JSON.stringify({ action: "terminate" }), currentSecretKey);
            const msgObj = {
                sender: myNickname,
                msgType: "control",
                payload: encryptedPayload
            };
            socket.send(JSON.stringify(msgObj));
            
            alert("Room terminated.");
            if (socket) socket.close();
            showView("view-welcome");
        }
    });
});

// Service Worker Registration for PWA
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('./sw.js')
            .then(reg => console.log('Service Worker registered', reg))
            .catch(err => console.log('Service Worker registration failed', err));
    });
}
