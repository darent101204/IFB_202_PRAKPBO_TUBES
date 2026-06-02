// Immediately set theme from localStorage to avoid flashing
(function() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);
})();

document.addEventListener('DOMContentLoaded', () => {
    const themeToggleBtn = document.getElementById('theme-toggle');
    if (themeToggleBtn) {
        updateToggleIcon(themeToggleBtn);

        themeToggleBtn.addEventListener('click', () => {
            const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
            const newTheme = currentTheme === 'light' ? 'dark' : 'light';
            
            document.documentElement.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
            
            updateToggleIcon(themeToggleBtn);
        });
    }
});

function updateToggleIcon(btn) {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
    const icon = btn.querySelector('i');
    if (icon) {
        if (currentTheme === 'dark') {
            icon.className = 'bi bi-sun';
        } else {
            icon.className = 'bi bi-moon';
        }
    }
}
