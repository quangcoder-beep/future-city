/**
 * FUTURE CITY - ADMIN DASHBOARD LOGIC
 * Includes Auto-refresh, Modal handling, and UX protection
 */

// --- SMART RELOAD LOGIC ---
let reloadTimer = null;
const RELOAD_INTERVAL = 60000; // 60 seconds
let isUserInteracting = false;

function startReloadTimer() {
    if (reloadTimer) clearTimeout(reloadTimer);
    reloadTimer = setTimeout(function () {
        // Only reload if no modal is open and no one is typing
        const activeModal = document.querySelector('.modal.show');
        const activeInput = document.activeElement;
        const isTyping = activeInput && (activeInput.tagName === 'INPUT' || activeInput.tagName === 'TEXTAREA');

        if (!activeModal && !isTyping && !isUserInteracting) {
            window.location.reload();
        } else {
            console.log("[UX] Reload postponed: User is interacting.");
            startReloadTimer(); // Wait for next cycle
        }
    }, RELOAD_INTERVAL);
}

// Detection of user activity to prevent annoying reloads
window.addEventListener('mousemove', () => { 
    isUserInteracting = true; 
    setTimeout(() => { isUserInteracting = false; }, 3000); 
});

window.addEventListener('keydown', () => { 
    isUserInteracting = true; 
    setTimeout(() => { isUserInteracting = false; }, 3000); 
});

// Initialize timer on load
document.addEventListener('DOMContentLoaded', startReloadTimer);

// --- BAN MODAL LOGIC ---
/**
 * Opens the ban confirmation modal with pre-filled data
 */
function openBanModal(id, name, userId, ip) {
    document.getElementById('ban_conn_id').value = id;
    document.getElementById('ban_user_id').value = userId;
    document.getElementById('ban_ip_addr').value = ip;
    document.getElementById('ban_player_name').value = name;
    
    // Using Bootstrap modal API
    const modalElement = document.getElementById('banModal');
    const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
    modal.show();
}

// --- ACTION CONFIRMATION MODAL LOGIC ---
document.addEventListener('DOMContentLoaded', function() {
    let formToSubmit = null;
    const genericModalElement = document.getElementById('genericConfirmModal');
    if (!genericModalElement) return; // Guard clause
    
    const genericModal = bootstrap.Modal.getOrCreateInstance(genericModalElement);
    const confirmBtn = document.getElementById('confirmModalConfirmBtn');
    const messageElement = document.getElementById('confirmModalMessage');

    // Intercept all forms with 'confirm-form' class
    document.addEventListener('submit', function(e) {
        if (e.target && e.target.classList.contains('confirm-form')) {
            e.preventDefault();
            formToSubmit = e.target;
            
            // Get custom message or use default
            const message = formToSubmit.getAttribute('data-message') || "Are you sure you want to proceed?";
            messageElement.textContent = message;
            
            genericModal.show();
        }
    });

    // Handle the actual confirmation button click
    if (confirmBtn) {
        confirmBtn.addEventListener('click', function() {
            if (formToSubmit) {
                formToSubmit.submit();
                genericModal.hide();
            }
        });
    }
});
