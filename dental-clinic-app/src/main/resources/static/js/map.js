var map;
var markers = [];
var userMarker;
var currentRouteLayer = null;
var userLat = null;
var userLon = null;
var currentMode = 'foot';

function initMap() {
    var center = [55.796, 49.115];

    map = new ymaps.Map('map', {
        center: center,
        zoom: 12,
        controls: ['zoomControl', 'fullscreenControl']
    });

    loadClinicsForMap();
}

function loadClinicsForMap() {
    fetch('/api/clinics')
        .then(function(response) { return response.json(); })
        .then(function(data) {
            for (var i = 0; i < data.length; i++) {
                var clinic = data[i];
                if (clinic.latitude && clinic.longitude) {
                    var marker = new ymaps.Placemark(
                        [clinic.latitude, clinic.longitude],
                        {
                            balloonContent: '<b>' + clinic.name + '</b><br>' + clinic.address + '<br><a href="/clinics/' + clinic.id + '">Подробнее</a>'
                        },
                        {
                            preset: 'islands#greenMedicalIcon'
                        }
                    );
                    map.geoObjects.add(marker);
                    markers.push({marker: marker, clinic: clinic});
                }
            }
        })
        .catch(function(error) { console.error(error); });
}

function setUserLocation(lat, lon) {
    userLat = lat;
    userLon = lon;

    if (userMarker) {
        map.geoObjects.remove(userMarker);
    }

    userMarker = new ymaps.Placemark([userLat, userLon], {
        balloonContent: 'Вы здесь'
    }, {
        preset: 'islands#blueCircleIcon'
    });

    map.geoObjects.add(userMarker);
    map.setCenter([userLat, userLon], 14);
    loadDistances();
}

function getUserLocation() {
    if (!navigator.geolocation) {
        alert('Геолокация не поддерживается');
        return;
    }

    var btn = document.getElementById('getLocationBtn');
    if (btn) {
        btn.innerHTML = '<span class="loading-indicator"></span> Определение...';
    }

    navigator.geolocation.getCurrentPosition(
        function(position) {
            setUserLocation(position.coords.latitude, position.coords.longitude);
            if (btn) btn.innerHTML = 'Определить';
        },
        function(error) {
            alert('Ошибка определения местоположения: ' + error.message);
            if (btn) btn.innerHTML = 'Определить';
        }
    );
}

function searchAddress() {
    var address = document.getElementById('addressInput').value;
    if (!address.trim()) {
        alert('Введите адрес');
        return;
    }

    var btn = document.getElementById('searchAddressBtn');
    if (btn) btn.innerHTML = '<span class="loading-indicator"></span> Поиск...';

    var url = 'https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(address) + '&limit=1';

    fetch(url)
        .then(function(response) { return response.json(); })
        .then(function(data) {
            if (data && data.length > 0) {
                setUserLocation(parseFloat(data[0].lat), parseFloat(data[0].lon));
                if (btn) btn.innerHTML = 'Найти';
            } else {
                alert('Адрес не найден');
                if (btn) btn.innerHTML = 'Найти';
            }
        })
        .catch(function(error) {
            alert('Ошибка поиска адреса');
            if (btn) btn.innerHTML = 'Найти';
        });
}

function loadDistances() {
    var url = '/api/distance/all?userLat=' + userLat + '&userLon=' + userLon + '&mode=' + currentMode;

    fetch(url)
        .then(function(response) { return response.json(); })
        .then(function(data) {
            var clinicList = document.getElementById('clinicList');
            if (clinicList) {
                var html = '';
                for (var i = 0; i < data.length && i < 15; i++) {
                    var d = data[i];
                    if (d.success) {
                        var dist = d.distance;
                        var distText = dist < 1 ? Math.round(dist * 1000) + ' м' : dist.toFixed(1) + ' км';
                        var modeText = currentMode === 'foot' ? 'Пешком' : 'На машине';
                        html += '<div class="clinic-item" onclick="buildRoute(' + d.clinic_id + ', \'' + d.clinic_name.replace(/'/g, "\\'") + '\', ' + d.latitude + ', ' + d.longitude + ')">';
                        html += '<div class="clinic-name">' + d.clinic_name + '</div>';
                        html += '<div class="distance">' + modeText + ': ' + distText + '</div>';
                        html += '</div>';
                    }
                }
                clinicList.innerHTML = html || '<div class="empty-state">Нет данных</div>';
            }
        })
        .catch(function(error) {
            console.error('Error loading distances:', error);
        });
}

function setMode(mode) {
    currentMode = mode;

    var btnFoot = document.getElementById('btnFoot');
    var btnDriving = document.getElementById('btnDriving');

    if (mode === 'foot') {
        if (btnFoot) btnFoot.classList.add('active');
        if (btnDriving) btnDriving.classList.remove('active');
    } else {
        if (btnDriving) btnDriving.classList.add('active');
        if (btnFoot) btnFoot.classList.remove('active');
    }

    clearRoute();
    if (userLat && userLon) {
        loadDistances();
    }
}

function clearRoute() {
    if (currentRouteLayer) {
        map.geoObjects.remove(currentRouteLayer);
        currentRouteLayer = null;
    }

    var routeInfo = document.getElementById('routeInfo');
    if (routeInfo) {
        routeInfo.innerHTML = '';
        routeInfo.style.display = 'none';
    }
}

function buildRoute(clinicId, clinicName, clinicLat, clinicLon) {
    clearRoute();

    var routeInfo = document.getElementById('routeInfo');
    if (!routeInfo) return;

    routeInfo.innerHTML = '<div class="loading-indicator"></div> Построение маршрута...';
    routeInfo.style.display = 'block';

    if (currentMode === 'driving') {
        var url = 'https://router.project-osrm.org/route/v1/driving/' + userLon + ',' + userLat + ';' + clinicLon + ',' + clinicLat + '?overview=full&geometries=geojson';

        fetch(url)
            .then(function(response) { return response.json(); })
            .then(function(data) {
                if (data.code === 'Ok' && data.routes && data.routes.length > 0) {
                    var route = data.routes[0];
                    var distance = route.distance / 1000;
                    var duration = route.duration / 60;
                    var distText = distance < 1 ? Math.round(distance * 1000) + ' м' : distance.toFixed(1) + ' км';
                    var durText = duration < 60 ? Math.round(duration) + ' мин' : Math.floor(duration / 60) + ' ч ' + Math.round(duration % 60) + ' мин';

                    var points = [];
                    var coordinates = route.geometry.coordinates;
                    for (var i = 0; i < coordinates.length; i++) {
                        points.push([coordinates[i][1], coordinates[i][0]]);
                    }

                    currentRouteLayer = new ymaps.Polyline(points, {
                        strokeColor: '#2196f3',
                        strokeWidth: 5
                    });
                    map.geoObjects.add(currentRouteLayer);

                    routeInfo.innerHTML = '<strong>На машине до ' + clinicName + '</strong><br>Расстояние: ' + distText + '<br>Время: ' + durText + '<br><button class="clear-route-btn" onclick="clearRoute()">Очистить</button>';
                } else {
                    routeInfo.innerHTML = 'Ошибка построения маршрута';
                }
            })
            .catch(function() {
                routeInfo.innerHTML = 'Ошибка соединения с сервером маршрутов';
            });
    } else {
        var distance = calculateDistance(userLat, userLon, clinicLat, clinicLon);
        var distText = distance < 1 ? Math.round(distance * 1000) + ' м' : distance.toFixed(1) + ' км';
        var duration = distance * 12;
        var durText = duration < 60 ? Math.round(duration) + ' мин' : Math.floor(duration / 60) + ' ч ' + Math.round(duration % 60) + ' мин';

        routeInfo.innerHTML = '<strong>Пешком до ' + clinicName + '</strong><br>Расстояние по прямой: ' + distText + '<br>Время: ' + durText + '<br><button class="clear-route-btn" onclick="clearRoute()">Очистить</button>';
    }
}

function calculateDistance(lat1, lon1, lat2, lon2) {
    var R = 6371;
    var dLat = (lat2 - lat1) * Math.PI / 180;
    var dLon = (lon2 - lon1) * Math.PI / 180;
    var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon/2) * Math.sin(dLon/2);
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
}

// Ждем загрузки DOM перед привязкой событий
document.addEventListener('DOMContentLoaded', function() {
    var getLocationBtn = document.getElementById('getLocationBtn');
    var btnFoot = document.getElementById('btnFoot');
    var btnDriving = document.getElementById('btnDriving');
    var searchAddressBtn = document.getElementById('searchAddressBtn');
    var addressInput = document.getElementById('addressInput');

    if (getLocationBtn) getLocationBtn.onclick = getUserLocation;
    if (btnFoot) btnFoot.onclick = function() { setMode('foot'); };
    if (btnDriving) btnDriving.onclick = function() { setMode('driving'); };
    if (searchAddressBtn) searchAddressBtn.onclick = searchAddress;
    if (addressInput) {
        addressInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchAddress();
            }
        });
    }
});

ymaps.ready(initMap);