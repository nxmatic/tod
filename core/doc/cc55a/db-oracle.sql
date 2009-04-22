DROP TABLE Events CASCADE CONSTRAINTS PURGE;
DROP TABLE FieldWrites CASCADE CONSTRAINTS PURGE;
DROP TABLE VarWrites CASCADE CONSTRAINTS PURGE;
DROP TABLE Args CASCADE CONSTRAINTS PURGE;
DROP TABLE BEnters CASCADE CONSTRAINTS PURGE;
DROP TABLE BExits CASCADE CONSTRAINTS PURGE;

CREATE TABLE Events (
	tid		NUMBER(20)
	, seq		NUMBER(20)
	, time		NUMBER(20)
	, type		NUMBER(1)
	, parentTid	NUMBER(20)
	, parentSeq	NUMBER(20)
	, PRIMARY KEY (tid, seq)
);

CREATE TABLE FieldWrites (
	tid		NUMBER(20)
	, seq		NUMBER(20)
	, fieldId	NUMBER(10)
	, target	NUMBER(20)
	, value		NUMBER(20)
	, PRIMARY KEY (tid, seq)
	, FOREIGN KEY (tid, seq) REFERENCES Events(tid, seq)
);

CREATE TABLE VarWrites (
	tid		NUMBER(20)
	, seq		NUMBER(20)
	, varId		NUMBER(10)
	, value		NUMBER(20)
	, PRIMARY KEY (tid, seq)
	, FOREIGN KEY (tid, seq) REFERENCES Events(tid, seq)
);

CREATE TABLE Args (
	id		NUMBER(20)
	, place		NUMBER(10)
	, value		NUMBER(20)
	, PRIMARY KEY (id, place)
);

CREATE TABLE BEnters (
	tid		NUMBER(20)
	, seq		NUMBER(20)
	, bhvId		NUMBER(10)
	, target	NUMBER(20)
	, argsId	NUMBER(20)
	, PRIMARY KEY (tid, seq)
	, FOREIGN KEY (tid, seq) REFERENCES Events(tid, seq)
);

CREATE TABLE BExits (
	tid		NUMBER(20)
	, seq		NUMBER(20)
	, retValue	NUMBER(20)
	, PRIMARY KEY (tid, seq)
	, FOREIGN KEY (tid, seq) REFERENCES Events(tid, seq)
);

ALTER TABLE Events ADD FOREIGN KEY (parentTid, parentSeq) REFERENCES BEnters(tid, seq);
