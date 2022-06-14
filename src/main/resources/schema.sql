CREATE TABLE IF NOT EXISTS proposals (id SERIAL PRIMARY KEY,
                                  title varchar(255) NOT NULL,
                                  description varchar(1300),
                                  author varchar(50) NOT NULL,
                                  email varchar(50) NOT NULL,
                                  approved boolean DEFAULT false,
                                  status varchar(10) NOT NULL
);