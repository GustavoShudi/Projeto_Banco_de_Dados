CREATE TABLE usuario (
	email VARCHAR(150) PRIMARY KEY,
	nome VARCHAR(150) NOT NULL,
	senha VARCHAR(100) NOT NULL,
	tipo VARCHAR(50) NOT NULL CHECK (tipo IN ('ALUNO', 'PROFESSOR'))
);

CREATE TABLE aluno (
	email_usuario VARCHAR(150) PRIMARY KEY,
	FOREIGN KEY (email_usuario) REFERENCES usuario(email) ON DELETE CASCADE
);

CREATE TABLE professor (
	email_usuario VARCHAR(150) PRIMARY KEY,
	FOREIGN KEY (email_usuario) REFERENCES usuario(email) ON DELETE CASCADE
);

CREATE TABLE materia (
	sigla VARCHAR(10) PRIMARY KEY,
	nome VARCHAR(100) NOT NULL
);

CREATE TABLE avaliacao (
	id_avaliacao BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	horario_inicio TIME NOT NULL,
	horario_fim TIME NOT NULL,
	descricao TEXT NOT NULL,
	titulo VARCHAR(100) NOT NULL,
	data_avaliacao DATE NOT NULL,
	email_professor VARCHAR(150) NOT NULL,
	sigla_materia VARCHAR(10) NOT NULL,

	FOREIGN KEY (email_professor) REFERENCES professor(email_usuario),
	FOREIGN KEY (sigla_materia) REFERENCES materia(sigla),
	CHECK (horario_fim > horario_inicio)
);

CREATE TABLE questao (
	id_questao BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	enunciado TEXT NOT NULL,
	tipo VARCHAR(50) NOT NULL CHECK (tipo IN ('OBJETIVA', 'SUBJETIVA'))
);

CREATE TABLE questao_subjetiva (
	id_questao BIGINT PRIMARY KEY,
	expectativa TEXT NOT NULL,
	FOREIGN KEY (id_questao) REFERENCES questao(id_questao) ON DELETE CASCADE
);

CREATE TABLE questao_objetiva (
	id_questao BIGINT PRIMARY KEY,
	FOREIGN KEY (id_questao) REFERENCES questao(id_questao) ON DELETE CASCADE
);

CREATE TABLE alternativa (
	id_questao_objetiva BIGINT,
	id_alternativa BIGINT GENERATED ALWAYS AS IDENTITY,
	texto TEXT NOT NULL,
	correta BOOLEAN NOT NULL DEFAULT FALSE,

	PRIMARY KEY (id_questao_objetiva, id_alternativa),
	FOREIGN KEY (id_questao_objetiva) REFERENCES questao_objetiva(id_questao) ON DELETE CASCADE
);

CREATE TABLE avaliacao_questao (
	id_avaliacao BIGINT,
	id_questao BIGINT,
	valor_questao NUMERIC(5,2) NOT NULL CHECK (valor_questao > 0),

	PRIMARY KEY (id_avaliacao, id_questao),
	FOREIGN KEY (id_avaliacao) REFERENCES avaliacao(id_avaliacao) ON DELETE CASCADE,
	FOREIGN KEY (id_questao) REFERENCES questao(id_questao)
);

CREATE TABLE realiza (
	id_avaliacao BIGINT,
	email_usuario VARCHAR(150),
	data_entrega TIMESTAMP,
	
	PRIMARY KEY (id_avaliacao, email_usuario),
	FOREIGN KEY (id_avaliacao) REFERENCES avaliacao(id_avaliacao),
	FOREIGN KEY (email_usuario) REFERENCES aluno(email_usuario)
);

CREATE TABLE responde (
	id_avaliacao BIGINT,
	email_usuario VARCHAR(150),
	id_questao BIGINT,
	resposta_subjetiva TEXT,
	id_alternativa_escolhida BIGINT,
	nota NUMERIC(5,2) CHECK (nota >= 0),
	
	PRIMARY KEY (id_avaliacao, email_usuario, id_questao),
	FOREIGN KEY (id_avaliacao, email_usuario) REFERENCES realiza(id_avaliacao, email_usuario),
	FOREIGN KEY (id_avaliacao, id_questao) REFERENCES avaliacao_questao(id_avaliacao, id_questao),
	FOREIGN KEY (id_questao, id_alternativa_escolhida) REFERENCES alternativa(id_questao_objetiva, id_alternativa)
);