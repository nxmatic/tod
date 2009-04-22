DROP TABLE Events CASCADE;
DROP TABLE FieldWrites CASCADE;
DROP TABLE VarWrites CASCADE;
DROP TABLE Args CASCADE;
DROP TABLE BEnters CASCADE;
DROP TABLE BExits CASCADE;

CREATE TABLE Events (
	tid		bigint,
	seq		bigint,
	time		bigint,
	type		smallint,
	parentTid	bigint,
	parentSeq	bigint--,

--	PRIMARY KEY (tid, seq)
);

CREATE TABLE FieldWrites (
	tid		bigint,
	seq		bigint,
	fieldId		integer,
	target		bigint,
	value		bigint--,

--	PRIMARY KEY (tid, seq)--,
--	FOREIGN KEY (tid, seq) REFERENCES Events(tid, seq)
);

CREATE TABLE VarWrites (
	tid		bigint,
	seq		bigint,
	varId		integer,
	value		bigint--,

--	PRIMARY KEY (tid, seq)--,
--	FOREIGN KEY (tid, seq) REFERENCES Events(tid, seq)
);

CREATE TABLE Args (
	id		bigint,
	index		integer,
	value		bigint--,
	
--	PRIMARY KEY (id, index)
);

CREATE TABLE BEnters (
	tid		bigint,
	seq		bigint,
	bhvId		integer,
	target		bigint,
	argsId		bigint--,

--	PRIMARY KEY (tid, seq)--,
--	FOREIGN KEY (tid, seq) REFERENCES Events(tid, seq)
);

CREATE TABLE BExits (
	tid		bigint,
	seq		bigint,
	retValue	bigint--,

--	PRIMARY KEY (tid, seq)--,
--	FOREIGN KEY (tid, seq) REFERENCES Events(tid, seq)
);

--ALTER TABLE Events ADD FOREIGN KEY (parentTid, parentSeq) REFERENCES BEnters(tid, seq);
