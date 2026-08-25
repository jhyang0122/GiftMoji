(function () {
	"use strict";

	const WALLET_KEY = "giftmoji_wallet";

	// ---------- tab navigation ----------

	const navButtons = document.querySelectorAll(".nav-btn");
	const screens = document.querySelectorAll(".screen");
	const screenTitle = document.getElementById("screenTitle");

	function showScreen(name) {
		screens.forEach((s) => s.classList.toggle("active", s.id === "screen-" + name));
		navButtons.forEach((b) => b.classList.toggle("active", b.dataset.screen === name));
		const active = document.querySelector('.nav-btn[data-screen="' + name + '"]');
		if (active) screenTitle.textContent = active.dataset.title;
		if (name === "wallet") renderWallet();
		if (name !== "redeem") stopScanning();
	}

	navButtons.forEach((btn) => {
		btn.addEventListener("click", () => showScreen(btn.dataset.screen));
	});

	// ---------- local wallet (per-device stand-in until real accounts exist) ----------

	function loadWallet() {
		try {
			return JSON.parse(localStorage.getItem(WALLET_KEY)) || [];
		} catch (e) {
			return [];
		}
	}

	function saveWallet(list) {
		localStorage.setItem(WALLET_KEY, JSON.stringify(list));
	}

	function addToWallet(voucher) {
		const list = loadWallet();
		list.unshift({ code: voucher.code, createdAt: voucher.createdAt, expiresAt: voucher.expiresAt });
		saveWallet(list);
	}

	function formatDate(iso) {
		if (!iso) return "";
		const d = new Date(iso);
		return isNaN(d) ? iso : d.toLocaleString();
	}

	function statusBadge(status) {
		const cls = status === "REDEEMED" ? "badge-redeemed" : status === "EXPIRED" ? "badge-expired" : "badge-issued";
		return '<span class="badge ' + cls + '">' + status + "</span>";
	}

	// ---------- send screen ----------

	const issueBtn = document.getElementById("issueBtn");
	const issuePanel = document.getElementById("issuePanel");
	const issueQr = document.getElementById("issueQr");
	const issueCode = document.getElementById("issueCode");
	const issueMeta = document.getElementById("issueMeta");
	const issueError = document.getElementById("issueError");
	const issueShareBtn = document.getElementById("issueShareBtn");

	issueBtn.addEventListener("click", async () => {
		issueError.textContent = "";
		issueBtn.disabled = true;
		issueBtn.textContent = "Creating...";
		try {
			const res = await fetch("/api/vouchers", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({}),
			});
			if (!res.ok) throw new Error("Server returned " + res.status);
			const voucher = await res.json();

			issueQr.src = voucher.qrUrl + "?t=" + Date.now();
			issueCode.textContent = voucher.code;
			issueMeta.textContent = "Expires " + formatDate(voucher.expiresAt);
			issuePanel.hidden = false;

			addToWallet(voucher);
		} catch (e) {
			issueError.textContent = "Couldn't create a voucher. Please try again.";
		} finally {
			issueBtn.disabled = false;
			issueBtn.textContent = "Create a gift voucher";
		}
	});

	issueShareBtn.addEventListener("click", () => shareCode(issueCode.textContent));

	async function shareCode(code) {
		if (navigator.share) {
			try {
				await navigator.share({ title: "GiftMoji voucher", text: code });
				return;
			} catch (e) {
				return; // user cancelled
			}
		}
		try {
			await navigator.clipboard.writeText(code);
			alert("Code copied: " + code);
		} catch (e) {
			alert("Code: " + code);
		}
	}

	// ---------- wallet screen ----------

	const walletList = document.getElementById("walletList");
	const walletEmpty = document.getElementById("walletEmpty");

	async function renderWallet() {
		const entries = loadWallet();
		walletEmpty.hidden = entries.length > 0;
		walletList.innerHTML = "";

		for (const entry of entries) {
			const card = document.createElement("div");
			card.className = "wallet-card";
			card.innerHTML =
				'<div class="wallet-card-head">' +
				'<span class="badge badge-issued">Loading...</span>' +
				'<button class="ghost-btn qr-toggle">Show QR</button>' +
				"</div>" +
				'<p class="code">' + entry.code + "</p>" +
				'<p class="meta">Expires ' + formatDate(entry.expiresAt) + "</p>" +
				'<img class="qr" hidden>';
			walletList.appendChild(card);

			const badge = card.querySelector(".badge");
			const qrToggle = card.querySelector(".qr-toggle");
			const qrImg = card.querySelector(".qr");

			qrToggle.addEventListener("click", () => {
				if (!qrImg.src) qrImg.src = "/api/vouchers/" + encodeURIComponent(entry.code) + "/qr?t=" + Date.now();
				qrImg.hidden = !qrImg.hidden;
				qrToggle.textContent = qrImg.hidden ? "Show QR" : "Hide QR";
			});

			try {
				const res = await fetch("/api/vouchers/" + encodeURIComponent(entry.code));
				if (res.ok) {
					const voucher = await res.json();
					badge.outerHTML = statusBadge(voucher.status);
				} else {
					badge.outerHTML = '<span class="badge badge-expired">UNKNOWN</span>';
				}
			} catch (e) {
				badge.outerHTML = '<span class="badge badge-expired">OFFLINE</span>';
			}
		}
	}

	// ---------- redeem screen ----------

	const redeemCodeInput = document.getElementById("redeemCode");
	const redeemBtn = document.getElementById("redeemBtn");
	const redeemResult = document.getElementById("redeemResult");
	const scanBtn = document.getElementById("scanBtn");
	const scanVideo = document.getElementById("scanVideo");

	redeemBtn.addEventListener("click", async () => {
		const code = redeemCodeInput.value.trim();
		if (!code) return;
		redeemResult.className = "result";
		redeemResult.textContent = "Redeeming...";
		redeemResult.style.display = "block";

		try {
			const res = await fetch("/api/vouchers/" + encodeURIComponent(code) + "/redeem", { method: "POST" });
			if (res.status === 404) {
				redeemResult.className = "result err";
				redeemResult.textContent = "No voucher found with that code.";
				return;
			}
			const data = await res.json();
			redeemResult.className = "result " + (res.ok ? "ok" : "err");
			redeemResult.textContent = data.message + " (" + data.status + ")";
		} catch (e) {
			redeemResult.className = "result err";
			redeemResult.textContent = "Couldn't reach the server. Check your connection and try again.";
		}
	});

	// Camera QR scanning where the browser supports it natively (Chrome/Android).
	// No BarcodeDetector on this browser (notably iOS Safari) -> manual entry only.
	let scanStream = null;
	let scanning = false;

	if ("BarcodeDetector" in window) {
		scanBtn.hidden = false;
	}

	scanBtn.addEventListener("click", () => {
		if (scanning) {
			stopScanning();
		} else {
			startScanning();
		}
	});

	async function startScanning() {
		try {
			scanStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } });
		} catch (e) {
			redeemResult.className = "result err";
			redeemResult.textContent = "Camera access was denied. You can still type the code in.";
			return;
		}
		scanVideo.srcObject = scanStream;
		scanVideo.hidden = false;
		await scanVideo.play();
		scanning = true;
		scanBtn.textContent = "Stop";

		const detector = new window.BarcodeDetector({ formats: ["qr_code"] });
		const loop = async () => {
			if (!scanning) return;
			try {
				const codes = await detector.detect(scanVideo);
				if (codes.length > 0) {
					redeemCodeInput.value = codes[0].rawValue;
					stopScanning();
					return;
				}
			} catch (e) {
				// transient decode error, keep trying
			}
			requestAnimationFrame(loop);
		};
		requestAnimationFrame(loop);
	}

	function stopScanning() {
		scanning = false;
		scanBtn.textContent = "Scan";
		scanVideo.hidden = true;
		if (scanStream) {
			scanStream.getTracks().forEach((t) => t.stop());
			scanStream = null;
		}
	}

	// ---------- service worker ----------

	if ("serviceWorker" in navigator) {
		window.addEventListener("load", () => {
			navigator.serviceWorker.register("/service-worker.js").catch(() => {});
		});
	}
})();
