function getCsrfToken() {
    var name = 'XSRF-TOKEN';
    var cookies = document.cookie.split(';');
    for (var i = 0; i < cookies.length; i++) {
        var cookie = cookies[i].trim();
        if (cookie.startsWith(name + '=')) {
            return decodeURIComponent(cookie.substring(name.length + 1));
        }
    }
    return null;
}

function showError(elementId, message) {
    var container = document.getElementById(elementId);
    if (container) {
        container.innerHTML = '<div class="error-message">' + message + '</div>';
    }
}

function showLoading(elementId) {
    var container = document.getElementById(elementId);
    if (container) {
        container.innerHTML = '<div class="loading">Поиск...</div>';
    }
}

function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/[&<>]/g, function(m) {
        if (m === '&') return '&amp;';
        if (m === '<') return '&lt;';
        if (m === '>') return '&gt;';
        return m;
    });
}

document.addEventListener('DOMContentLoaded', function() {
    var searchInput = document.getElementById('ajaxDoctorSearch');
    var resultsDiv = document.getElementById('doctorSearchResults');

    if (searchInput) {
        var timeout = null;

        searchInput.addEventListener('input', function() {
            clearTimeout(timeout);
            var query = this.value.trim();

            if (query.length < 2) {
                if (resultsDiv) resultsDiv.innerHTML = '';
                return;
            }

            timeout = setTimeout(function() {
                showLoading('doctorSearchResults');
                var url = '/ajax/doctors/by-specialization?specialization=' + encodeURIComponent(query);

                fetch(url, {
                    method: 'GET',
                    credentials: 'same-origin',
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                })
                .then(function(response) {
                    if (!response.ok) {
                        throw new Error('HTTP error');
                    }
                    return response.json();
                })
                .then(function(data) {
                    if (resultsDiv) {
                        if (data.success && data.doctors && data.doctors.length > 0) {
                            var html = '<div class="doctors-grid">';
                            for (var i = 0; i < data.doctors.length; i++) {
                                var doctor = data.doctors[i];
                                html += '<div class="doctor-card">';
                                if (doctor.photoUrl) {
                                    html += '<img src="' + doctor.photoUrl + '" class="doctor-photo" alt="doctor">';
                                } else {
                                    html += '<div class="doctor-photo-placeholder">Д</div>';
                                }
                                html += '<h3>' + escapeHtml(doctor.name) + '</h3>';
                                html += '<p>' + escapeHtml(doctor.specialization) + '</p>';
                                if (doctor.experience > 0) {
                                    html += '<p>' + doctor.experience + ' лет опыта</p>';
                                }
                                html += '</div>';
                            }
                            html += '</div>';
                            resultsDiv.innerHTML = html;
                        } else {
                            resultsDiv.innerHTML = '<div class="info-text">Врачи не найдены</div>';
                        }
                    }
                })
                .catch(function(error) {
                    console.error('Search error:', error);
                    if (resultsDiv) {
                        resultsDiv.innerHTML = '<div class="error-message">Ошибка поиска</div>';
                    }
                });
            }, 300);
        });
    }
});