CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
	Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Appointments (
    apID int PRIMARY KEY,
    cUsername varchar(255),
    pUsername varchar(255),
    vName varchar(255),
    FOREIGN KEY(cUsername) REFERENCES Caregivers(username),
    FOREIGN KEY(pUsername) REFERENCES Patients(username),
    FOREIGN KEY(vName) REFERENCES Vaccines(name)
);