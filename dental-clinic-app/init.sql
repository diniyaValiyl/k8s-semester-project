-- 1. СОЗДАНИЕ ТАБЛИЦ

-- Таблица пользователей
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('PATIENT', 'ADMIN', 'CLINIC_ADMIN')),
    managed_clinic_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица клиник
CREATE TABLE clinics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(300) NOT NULL,
    phone VARCHAR(50),
    hours VARCHAR(200),
    site_url VARCHAR(200),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    logo_url VARCHAR(500),
    work_start TIME DEFAULT '09:00',
    work_end TIME DEFAULT '20:00',
    work_days VARCHAR(50) DEFAULT 'MON,TUE,WED,THU,FRI',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица врачей
CREATE TABLE doctors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    specialization VARCHAR(200),
    experience INTEGER,
    education TEXT,
    photo_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Связь врачей с клиниками
CREATE TABLE clinic_doctors (
    clinic_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    PRIMARY KEY (clinic_id, doctor_id),
    FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- Таблица услуг
CREATE TABLE dental_services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Связь клиник с услугами
CREATE TABLE clinic_services (
    clinic_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    price DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (clinic_id, service_id),
    FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES dental_services(id) ON DELETE CASCADE
);

-- Таблица отзывов
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    patient_id BIGINT NOT NULL,
    clinic_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE
);

-- Таблица записей на приём
CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    date_time TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    clinic_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE
);

-- Таблица медицинских карт
CREATE TABLE medical_records (
    id BIGSERIAL PRIMARY KEY,
    diagnosis VARCHAR(300),
    notes TEXT,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- Избранные клиники
CREATE TABLE favorite_clinics (
    user_id BIGINT NOT NULL,
    clinic_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, clinic_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE
);

-- 2. ВНЕШНИЕ КЛЮЧИ

ALTER TABLE users ADD CONSTRAINT fk_users_managed_clinic
    FOREIGN KEY (managed_clinic_id) REFERENCES clinics(id) ON DELETE SET NULL;

-- 3. ЗАПОЛНЕНИЕ КЛИНИК

INSERT INTO clinics (id, name, address, phone, hours, site_url, latitude, longitude, logo_url, work_start, work_end, work_days) VALUES
(1, 'Вильдан', 'просп. Ямашева, 43', '+7 (917) 909-17-17', 'Пн–Пт 08:00–20:00, Сб 08:00–14:00', NULL, 55.826276, 49.117832, '/images/clinics/vildan.jpg', '08:00', '20:00', 'MON,TUE,WED,THU,FRI,SAT'),
(2, 'Зубная лечебница', 'просп. Альберта Камалеева, 12', '+7 (843) 291-63-41', 'Пн–Пт 08:00–20:00, Сб–Вс 09:00–15:00', 'https://zubnal.ru', 55.789892, 49.183525, '/images/clinics/zubnaya.jpg', '08:00', '20:00', 'MON,TUE,WED,THU,FRI,SAT,SUN'),
(3, 'Дентиатр', 'ул. Некрасова, 7А', '+7 (966) 250-61-61', 'Пн–Пт 08:00–20:00, Сб 09:00–15:00', 'https://dentiatr.ru', 55.788788, 49.124713, '/images/clinics/dentiatr.jpg', '08:00', '20:00', 'MON,TUE,WED,THU,FRI,SAT'),
(4, 'Хаят смайл', 'ул. Галактионова, 3', 'запись через сайт', 'Пн–Пт 09:00–20:00, Сб 09:00–18:00', NULL, 55.793232, 49.126626, '/images/clinics/hayat.jpg', '09:00', '18:00', 'MON,TUE,WED,THU,FRI,SAT'),
(5, 'Рокада Мед', 'Петербургская ул., 26', '+7 (843) 208-54-48', 'Пн–Пт 08:00–20:00, Сб 09:00–16:00', 'https://clinic.rocadamed.ru', 55.784496, 49.126554, '/images/clinics/rocada.jpg', '08:00', '16:00', 'MON,TUE,WED,THU,FRI,SAT'),
(6, 'КГМУ Стоматологическая поликлиника', 'ул. Бутлерова, 16', '+7 (843) 236-14-98', 'Пн–Пт 08:00–20:00, Сб 08:00–14:00', 'https://stomkgmu.ru', 55.787523, 49.130228, '/images/clinics/kgmu.jpg', '08:00', '14:00', 'MON,TUE,WED,THU,FRI,SAT'),
(7, 'Булгар-Стом (центр)', 'ул. Право-Булачная, 37', '8 (843) 292-00-99', 'Пн–Сб 09:00–20:00', 'https://bulgar-stom.ru', 55.789563, 49.111723, '/images/clinics/bulgar.jpg', '09:00', '20:00', 'MON,TUE,WED,THU,FRI,SAT'),
(8, 'Булгар-Стом (Фэмили Стом)', 'ул. Рауиса Гареева, 92', '8 (960) 040-88-88', 'Пн–Сб 09:00–20:00', 'https://bulgar-stom.ru', 55.722416, 49.171506, '/images/clinics/bulgar2.jpg', '09:00', '20:00', 'MON,TUE,WED,THU,FRI,SAT'),
(9, 'Занифдент', 'Повстанческая ул., 12', '+7 (987) 225-57-22', 'ежедневно 09:00–20:00', 'https://zanifdent-med.obiz.ru', 55.818437, 49.063331, '/images/clinics/zanif.jpg', '09:00', '20:00', 'MON,TUE,WED,THU,FRI,SAT,SUN'),
(10, 'Вайт Бьюти', 'ул. Аделя Кутуя, 44А', '+7 (930) 333-41-71', 'ежедневно 09:00–21:00', 'https://whitebeauty-stom.ru', 55.826256, 49.111184, '/images/clinics/whitebeauty.jpg', '09:00', '21:00', 'MON,TUE,WED,THU,FRI,SAT,SUN'),
(11, 'ДИН Дентал Клиник', 'ул. Космонавтов, 39', '+7 (901) 143-27-39', 'Пн–Пт 08:00–20:00, Сб 09:00–15:00', 'https://din-dental-clinic-med.ru', 55.799887, 49.192374, '/images/clinics/din.jpg', '08:00', '15:00', 'MON,TUE,WED,THU,FRI,SAT');

-- 4. ЗАПОЛНЕНИЕ ВРАЧЕЙ

INSERT INTO doctors (id, name, specialization, experience, education, photo_url) VALUES
(1, 'Валиуллина Райхана Расиховна', 'Стоматолог-терапевт', 6, 'Лечение кариеса, пульпита, периодонтита', '/images/doctors/raikhana.jpg'),
(2, 'Эргашев Хустнудин Оралович', 'Стоматолог-хирург', 13, 'Полный спектр стоматологических услуг', '/images/doctors/ergashev.jpg'),
(3, 'Вильданов Рашид Фасахович', 'Стоматолог-ортопед', 36, 'Директор клиники', '/images/doctors/rashid.jpg'),
(4, 'Вильданова Аделя Рашидовна', 'Стоматолог-терапевт', 3, 'Стоматолог-терапевт', '/images/doctors/adelya.jpg'),
(5, 'Вильданова Эмилия Рашидовна', 'Стоматолог-ортопед', 8, 'Заместитель директора', '/images/doctors/emilia.jpg'),
(6, 'Рафф Алла Ибрагимовна', 'Стоматолог-ортодонт', 16, 'Исправление прикуса, брекет-системы', '/images/doctors/raff.jpg'),
(7, 'Хабиров Камиль Анзяпович', 'Хирург-имплантолог', NULL, 'Генеральный директор', '/images/doctors/khabirov.jpg'),
(8, 'Загидуллина Алина Александровна', 'Детский стоматолог', NULL, 'Детская стоматология', '/images/doctors/zagidullina.jpg'),
(9, 'Хабиров Эмиль Камилевич', 'Стоматолог-ортопед', NULL, 'Исполнительный директор', '/images/doctors/khabirov2.jpg'),
(10, 'Смирнова Алена Андреевна', 'Ортодонт', NULL, 'Ортодонтия', '/images/doctors/smirnova.jpg'),
(11, 'Ахметова Диляра Мукатдисовна', 'Стоматолог-ортопед', 23, 'Главный врач', '/images/doctors/akhmetova.jpg'),
(12, 'Зубкова Елена Эдуардовна', 'Стоматолог-ортодонт', 40, 'Врач высшей категории', '/images/doctors/zubkova.jpg'),
(13, 'Мансуров Булат Фаридович', 'Стоматолог-хирург', 9, 'Хирургическая стоматология', '/images/doctors/mansurov.jpg'),
(14, 'Салимзянова Диляра Наилевна', 'Стоматолог-терапевт', 30, 'Эстетическая реставрация', '/images/doctors/salimzyanova.jpg'),
(15, 'Рамазанов Магомедрасул Рабаданович', 'Стоматолог-имплантолог', 21, 'Основатель и главный врач', '/images/doctors/ramazanov.jpg'),
(16, 'Мусина Индира Ивановна', 'Стоматолог-терапевт', 17, 'Терапия и детская стоматология', '/images/doctors/musina.jpg'),
(17, 'Токаев Нурлан Каржаубаевич', 'Челюстно-лицевой хирург', 4, 'Челюстно-лицевая хирургия', '/images/doctors/tokaev.jpg'),
(18, 'Ахметов Раис Эмирзянович', 'Стоматолог-ортопед', 16, 'Ведущий специалист', '/images/doctors/akhmetov.jpg'),
(19, 'Бутаева Зарина Ризвановна', 'Стоматолог-терапевт', 9, 'КГМУ 2015', '/images/doctors/butaeva.jpg'),
(20, 'Хусаенова Гульнара Ильдаровна', 'Детский стоматолог', 8, 'КГМУ 2016', '/images/doctors/khuseinova.jpg'),
(21, 'Савченко Марина Витальевна', 'Ортодонт', 21, 'КГМУ 2003', '/images/doctors/savchenko.jpg'),
(22, 'Шакиров Эдуард Юрьевич', 'Стоматолог-ортопед', 6, 'КГМУ 2018', '/images/doctors/shakirov.jpg'),
(23, 'Серякова Галина Евгеньевна', 'Стоматолог-терапевт', 39, 'Высшая категория', '/images/doctors/seryakova.jpg'),
(24, 'Галиев Илгизар Мансурович', 'Стоматолог-хирург', 31, 'КГМУ 1993', '/images/doctors/galiev.jpg'),
(25, 'Салеева Гульшат Тауфиковна', 'Стоматолог-ортопед', 34, 'Профессор', '/images/doctors/saleeva.jpg'),
(26, 'Абдуллина Зульфия Вафовна', 'Ортодонт', 46, 'КГМУ 1978', '/images/doctors/abdullina.jpg'),
(27, 'Бедертдинов Башир Нуртдинович', 'Стоматолог-ортопед', NULL, 'Ортопедическая стоматология', '/images/doctors/bedertdinov.jpg'),
(28, 'Маяков Олег Александрович', 'Стоматолог-хирург', NULL, 'Хирургическая стоматология', '/images/doctors/mayakov.jpg'),
(29, 'Мередова Алсу Язгельдиевна', 'Стоматолог-терапевт', NULL, 'Терапевтическая стоматология', '/images/doctors/meredova.jpg'),
(30, 'Файзуллин Эмиль Айдарович', 'Стоматолог-хирург', NULL, 'Хирургическая стоматология', '/images/doctors/fayzullin.jpg'),
(31, 'Халитов Заниф Марварович', 'Стоматолог-ортопед', NULL, 'Ортопедия, хирургия', '/images/doctors/khalitov.jpg'),
(32, 'Комарова Роза Константиновна', 'Стоматолог-терапевт', 20, 'Терапевтическая стоматология', '/images/doctors/komarova.jpg'),
(33, 'Ахунзянов Ленар Ильхамович', 'Стоматолог-терапевт', NULL, 'Высшая категория', '/images/doctors/akhunzyanov.jpg'),
(34, 'Акаев Николай Алексеевич', 'Стоматолог-ортопед', NULL, 'Ортопедическая стоматология', '/images/doctors/akaev.jpg'),
(35, 'Галеев Азат Рафикович', 'Хирург-имплантолог', 9, 'ТОП-25 докторов', '/images/doctors/galeyev.jpg'),
(36, 'Аль-Хазаали Раафат Моханад', 'Хирург-имплантолог', 5, 'All-on-4', '/images/doctors/alhazaali.jpg'),
(37, 'Мушарапов Асхат Рустэмович', 'Стоматолог-имплантолог', 10, 'Имплантология', '/images/doctors/musharapov.jpg'),
(38, 'Мушарапова Фарида Минегалиевна', 'Стоматолог-ортопед', 39, 'Высшая категория', '/images/doctors/musharapova.jpg'),
(39, 'Зиннурова Саида Мансуровна', 'Стоматолог-хирург', 39, 'Хирургическая стоматология', '/images/doctors/zinnurova.jpg'),
(40, 'Салимуллина Ильвира Ренатовна', 'Ортодонт', 31, 'Виниры, брекеты', '/images/doctors/salimullina.jpg');

-- 5. СВЯЗЬ ВРАЧЕЙ С КЛИНИКАМИ

INSERT INTO clinic_doctors (clinic_id, doctor_id) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),
(2,7),(2,8),(2,9),(2,10),
(3,11),(3,12),(3,13),(3,14),
(4,15),(4,16),(4,17),(4,18),
(5,19),(5,20),(5,21),(5,22),
(6,23),(6,24),(6,25),(6,26),
(7,27),(7,28),(7,29),(7,30),
(8,27),(8,28),(8,29),(8,30),
(9,31),(9,32),(9,33),(9,34),
(10,35),(10,36),
(11,37),(11,38),(11,39),(11,40);

-- 6. ЗАПОЛНЕНИЕ УСЛУГ

INSERT INTO dental_services (id, name, description, price) VALUES
(1, 'Консультация стоматолога', 'Осмотр, консультация', NULL),
(2, 'Лечение кариеса', 'Лечение среднего кариеса', NULL),
(3, 'Лечение пульпита', 'Эндодонтическое лечение', NULL),
(4, 'Удаление зуба простое', 'Экстракция зуба', NULL),
(5, 'Удаление зуба сложное', 'Удаление зуба мудрости', NULL),
(6, 'Профессиональная гигиена', 'Ультразвуковая чистка', NULL),
(7, 'Металлокерамическая коронка', 'Изготовление и установка', NULL),
(8, 'Керамическая коронка', 'E-MAX, цирконий', NULL),
(9, 'Керамический винир', 'Эстетическая реставрация', NULL),
(10, 'Металлические брекеты', 'Установка на одну челюсть', NULL),
(11, 'Керамические брекеты', 'Эстетическая брекет-система', NULL),
(12, 'Имплантация корейская', 'Установка имплантата', NULL),
(13, 'Имплантация швейцарская', 'Премиальный имплантат', NULL),
(14, 'Отбеливание зубов', 'Кабинетное отбеливание', NULL),
(15, 'Прием детского стоматолога', 'Консультация и осмотр', NULL);

-- 7. ЦЕНЫ НА УСЛУГИ ПО КЛИНИКАМ

INSERT INTO clinic_services (clinic_id, service_id, price) VALUES
(4,2,2500),(4,3,3000),(4,6,3000),(4,4,2500),(4,5,5000),
(4,12,20000),(4,13,25000),(4,10,36300),(4,11,68000),
(4,9,20000),(4,14,39999),(4,15,400),
(7,2,6600),(7,3,9000),(7,5,9900),(7,8,25000),(7,10,65000),
(8,2,6600),(8,3,9000),(8,5,9900),(8,8,25000),(8,10,65000),
(9,1,0),(9,6,5000),(9,9,18000),(9,7,5500),(9,4,1000),
(10,2,4950),(10,10,80000),(10,7,16500),(10,13,24000),(10,12,19900),
(11,1,300),(11,4,2000),(11,5,3000),(11,7,12000),(11,9,35000);

--8. Пользователи (пароль 123456 для всех)
INSERT INTO users (name, email, password, role, managed_clinic_id) VALUES
('Администратор', 'admin@dental.ru', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'ADMIN', NULL),
('Иван Петров', 'ivan@example.com', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'PATIENT', NULL),
('Мария Сидорова', 'maria@example.com', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'PATIENT', NULL);

--9. Админы клиник (пароль 123456)
INSERT INTO users (name, email, password, role, managed_clinic_id) VALUES
('Админ Вильдан', 'vildan@dental.ru', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'CLINIC_ADMIN', 1),
('Админ Зубная', 'zubnaya@dental.ru', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'CLINIC_ADMIN', 2),
('Админ Дентиатр', 'dentiatr@dental.ru', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'CLINIC_ADMIN', 3),
('Админ Хаят', 'hayat@dental.ru', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'CLINIC_ADMIN', 4),
('Админ Рокада', 'rocada@dental.ru', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'CLINIC_ADMIN', 5),
('Админ КГМУ', 'kgmu@dental.ru', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'CLINIC_ADMIN', 6),
('Админ Булгар', 'bulgar@dental.ru', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'CLINIC_ADMIN', 7),
('Админ Занифдент', 'zanif@dental.ru', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'CLINIC_ADMIN', 9),
('Админ Вайт Бьюти', 'whitebeauty@dental.ru', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'CLINIC_ADMIN', 10),
('Админ ДИН', 'din@dental.ru', '$2a$10$J0xhBRNqeKK2Trz4GdxLTOWZxZ2dkpaTTKHEeL/gtl.kqaDac0HZK', 'CLINIC_ADMIN', 11);
-- 10. ОТЗЫВЫ

INSERT INTO reviews (rating, comment, patient_id, clinic_id) VALUES
(5, 'Отличная клиника, вежливый персонал', 2, 1),
(4, 'Хорошие врачи, но дороговато', 2, 3),
(5, 'Лучшая стоматология в городе', 3, 4),
(4, 'Нормальная клиника, запись по времени', 3, 2),
(5, 'Очень доволен лечением', 2, 7),
(3, 'Долго ждал приёма, но лечение качественное', 3, 5);

-- 11. ЗАПИСИ НА ПРИЁМ

INSERT INTO appointments (date_time, status, patient_id, doctor_id, clinic_id) VALUES
('2025-05-15 10:00:00', 'CONFIRMED', 2, 1, 1),
('2025-05-16 14:30:00', 'PENDING', 2, 11, 3),
('2025-05-18 11:00:00', 'CONFIRMED', 3, 15, 4),
('2025-05-20 09:00:00', 'COMPLETED', 2, 19, 5),
('2025-05-22 15:00:00', 'CANCELLED', 3, 23, 6);


-- 12. ИЗБРАННЫЕ КЛИНИКИ

INSERT INTO favorite_clinics (user_id, clinic_id) VALUES
(2,1),(2,3),(2,7),(3,4),(3,7);

-- 13. ИНДЕКСЫ

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_appointments_date ON appointments(date_time);
CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor ON appointments(doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointments_clinic ON appointments(clinic_id);
CREATE INDEX IF NOT EXISTS idx_reviews_clinic ON reviews(clinic_id);
CREATE INDEX IF NOT EXISTS idx_doctors_specialization ON doctors(specialization);
CREATE INDEX IF NOT EXISTS idx_clinic_doctors_clinic ON clinic_doctors(clinic_id);
CREATE INDEX IF NOT EXISTS idx_clinic_doctors_doctor ON clinic_doctors(doctor_id);
CREATE INDEX IF NOT EXISTS idx_users_managed_clinic ON users(managed_clinic_id);

-- 14. СБРОС ПОСЛЕДОВАТЕЛЬНОСТЕЙ

SELECT setval('clinics_id_seq', (SELECT COALESCE(MAX(id), 1) FROM clinics));
SELECT setval('doctors_id_seq', (SELECT COALESCE(MAX(id), 1) FROM doctors));
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('appointments_id_seq', (SELECT COALESCE(MAX(id), 1) FROM appointments));
SELECT setval('dental_services_id_seq', (SELECT COALESCE(MAX(id), 1) FROM dental_services));
SELECT setval('reviews_id_seq', (SELECT COALESCE(MAX(id), 1) FROM reviews));
SELECT setval('medical_records_id_seq', (SELECT COALESCE(MAX(id), 1) FROM medical_records));