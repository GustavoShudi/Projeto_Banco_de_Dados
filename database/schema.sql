CREATE TABLE usuario (
	email VARCHAR(150) PRIMARY KEY,
	nome VARCHAR(150) NOT NULL,
	senha VARCHAR(100) NOT NULL,
	tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('ALUNO', 'PROFESSOR'))
);

CREATE TABLE aluno (
	email_usuario VARCHAR(150) PRIMARY KEY,
	FOREIGN KEY(email_usuario)
	REFERENCES usuario(email)
	ON DELETE CASCADE
);

CREATE TABLE professor (
	email_usuario VARCHAR(150) PRIMARY KEY,
	FOREIGN KEY(email_usuario)
	REFERENCES usuario(email)
	ON DELETE CASCADE
);

CREATE TABLE materia (
	sigla VARCHAR(4) PRIMARY KEY,
	nome VARCHAR(100) NOT NULL
);


CREATE TABLE possui (
	email_usuario VARCHAR(150),
    sigla_materia VARCHAR(4),
    
    PRIMARY KEY (email_usuario, sigla_materia),
    FOREIGN KEY (email_usuario)
    REFERENCES professor(email_usuario),
    FOREIGN KEY (sigla_materia)
    REFERENCES materia(sigla)
);

CREATE TABLE avaliacao (
	id_avaliacao BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	horario_inicio TIME NOT NULL,
	horario_fim TIME NOT NULL,
	descricao TEXT NOT NULL,
	titulo VARCHAR(100) NOT NULL,
	data_avaliacao DATE NOT NULL,
	email_professor VARCHAR(150) NOT NULL,
	sigla_materia VARCHAR(4) NOT NULL,

	FOREIGN KEY (email_professor, sigla_materia)
    REFERENCES possui(email_usuario, sigla_materia),

	CHECK(horario_fim > horario_inicio)
);

CREATE TABLE questao (
	id_questao BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	enunciado TEXT NOT NULL,
	tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('OBJETIVA', 'SUBJETIVA'))
);

CREATE TABLE questao_subjetiva (
	id_questao BIGINT PRIMARY KEY,
	expectativa TEXT NOT NULL,
	FOREIGN KEY (id_questao)
	REFERENCES questao(id_questao)
	ON DELETE CASCADE
);

CREATE TABLE questao_objetiva (
	id_questao BIGINT PRIMARY KEY,
	FOREIGN KEY (id_questao)
	REFERENCES questao(id_questao)
	ON DELETE CASCADE
);

CREATE TABLE alternativa (
	id_questao_objetiva BIGINT,
	id_alternativa BIGINT GENERATED ALWAYS AS IDENTITY,
	texto TEXT NOT NULL,
	correta BOOLEAN NOT NULL DEFAULT FALSE,

	PRIMARY KEY (id_questao_objetiva, id_alternativa),
	FOREIGN KEY (id_questao_objetiva)
	REFERENCES questao_objetiva(id_questao)
	ON DELETE CASCADE

);

CREATE UNIQUE INDEX unica_alternativa_correta
	ON alternativa (id_questao_objetiva)
	WHERE correta = TRUE;

CREATE TABLE avaliacao_questao (
	id_avaliacao BIGINT NOT NULL,
    id_questao BIGINT NOT NULL,
    valor_questao NUMERIC(8,2) NOT NULL CHECK (valor_questao > 0),
    PRIMARY KEY (id_avaliacao, id_questao),
    FOREIGN KEY (id_avaliacao)
    REFERENCES avaliacao(id_avaliacao),
    FOREIGN KEY (id_questao)
    REFERENCES questao(id_questao)
);

CREATE TABLE realiza (
	id_avaliacao BIGINT NOT NULL,
    email_aluno VARCHAR(150) NOT NULL,
	nota_total NUMERIC(8,2) NOT NULL DEFAULT 0,
    
    PRIMARY KEY (id_avaliacao, email_aluno),
    FOREIGN KEY (id_avaliacao)
    REFERENCES avaliacao(id_avaliacao),
    FOREIGN KEY (email_aluno)
    REFERENCES aluno(email_usuario)
);

CREATE TABLE responde (
	id_avaliacao BIGINT NOT NULL,
    email_aluno VARCHAR(150) NOT NULL,
    id_questao BIGINT NOT NULL,
    resposta TEXT NOT NULL,
    nota NUMERIC(8,2) NOT NULL CHECK (nota >= 0),
    
    PRIMARY KEY (id_avaliacao, email_aluno, id_questao),
    FOREIGN KEY (id_avaliacao, email_aluno)
    REFERENCES realiza(id_avaliacao, email_aluno),
    FOREIGN KEY (id_avaliacao, id_questao)
    REFERENCES avaliacao_questao(id_avaliacao, id_questao)
);

