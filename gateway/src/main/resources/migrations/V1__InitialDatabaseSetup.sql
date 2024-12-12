CREATE TABLE users (
  username varchar PRIMARY KEY,
  password varchar NOT NULL
);

CREATE TABLE admin (
  username varchar PRIMARY KEY REFERENCES users
);

CREATE TABLE participant (
  username varchar PRIMARY KEY REFERENCES users,
  email varchar UNIQUE NOT NULL
);

CREATE TABLE problem (
  id serial PRIMARY KEY,
  description varchar NOT NULL
);

CREATE TABLE submission_status (
  id serial PRIMARY KEY,
  name varchar NOT NULL
);

INSERT INTO submission_status (name) VALUES
  ('processing'),
  ('ok'),
  ('wrong answer');

CREATE TABLE submission_draft (
  id serial PRIMARY KEY,
  problem_id integer REFERENCES problem,
  answer VARCHAR NOT NULL,
  username varchar REFERENCES participant
);

CREATE TABLE submission (
  id integer PRIMARY KEY REFERENCES submission_draft,
  status_id integer REFERENCES submission_status
);