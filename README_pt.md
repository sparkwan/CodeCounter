﻿# NeoCodeTools

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)

🌐 **README em outros idiomas:**
[English](README.md) | [简体中文](README_zh_CN.md) | [繁體中文](README_zh_TW.md) | [日本語](README_ja.md) | [Español](README_es.md) | [Deutsch](README_de.md) | [Français](README_fr.md)

**NeoCodeTools** é uma ferramenta de desktop gratuita, modular e extensível para engenheiros de software e equipes. Construída sobre uma arquitetura de plugins, fornece um conjunto de utilitários de análise e transformação de código-fonte através de uma interface gráfica Swing intuitiva.

---

## 📸 Capturas de Tela

![NeoCodeTools Captura de Tela](screenshorts/screenshorts_pt.png)

---

## 🎬 Vídeo Tutorial

<video src="videos/tutorial.mp4" controls width="800"></video>

> Se o vídeo não for reproduzido no seu navegador, você pode [baixá-lo diretamente](videos/tutorial.mp4).

---

## ✨ Funcionalidades

### 🔌 Arquitetura de Plugins
- Design completamente modular — adicione novas ferramentas sem modificar o núcleo
- Cada plugin é executado em sua própria aba com interface independente
- Gerenciamento do ciclo de vida dos plugins (inicialização / encerramento)

### 📊 Plugin Contador de Código
- Conta **linhas de código**, **linhas de comentários**, **linhas em branco** e marcadores **TODO**
- Modelos de tipos de arquivo: Java, Java Web, Java Backend, Frontend, Python, Web e Personalizado
- Conjuntos predefinidos de diretórios excluídos:
  - **VCS**: `.git`, `.svn`, `.hg`
  - **IDE / Editor**: `.idea`, `.settings`, `.vscode`, `.project`, `.classpath`
  - **Build / Projeto**: `target`, `build`, `dist`, `node_modules`, `__pycache__`
- Tabela de resultados com **paginação**
- **Gráficos**: gráfico de barras (comparação de arquivos) e gráfico de pizza (resumo)
- **Exportação**: CSV, XLSX, PDF (com suporte a fontes CJK), Word (DOCX)

### 🔧 Plugin Formatador de Código *(em desenvolvimento)*
- Formatação em lote de arquivos de código-fonte

### 📦 Plugin Renomeação de Pacotes *(em desenvolvimento)*
- Renomeação em lote de pacotes / namespaces em um projeto

### 🌍 Internacionalização (i18n)
Completamente localizado em **8 idiomas**:
| Idioma | |
|--------|---|
| English (Inglês) | 🇬🇧 |
| 简体中文 (Chinês simplificado) | 🇨🇳 |
| 繁體中文 (Chinês tradicional) | 🇹🇼 |
| 日本語 (Japonês) | 🇯🇵 |
| Español (Espanhol) | 🇪🇸 |
| Deutsch (Alemão) | 🇩🇪 |
| Français (Francês) | 🇫🇷 |
| Português | 🇧🇷 |

O idioma é detectado automaticamente a partir da localidade do SO na inicialização.

### 🎨 Temas
- Temas **claro** e **escuro** com [FlatLaf](https://www.formdev.com/flatlaf/)
- Tema escuro Darcula estilo IntelliJ
- Alternância com um clique no menu *Exibir*

---

## 🚀 Primeiros passos

### Pré-requisitos
- **Java 17** ou superior
- **Maven 3.6+**

### Compilar
```bash
mvn clean package
```

### Executar
```bash
java -jar target/source-0.0.1-SNAPSHOT.jar
```

---

## 🏗️ Estrutura do projeto

```
source/
├── pom.xml
├── LICENSE
├── README.md
└── src/
    ├── main/
    │   ├── java/com/github/dev/tool/
    │   │   ├── PluginHostApplication.java       # Janela principal
    │   │   ├── plugin/                           # API do framework de plugins
    │   │   │   ├── Plugin.java
    │   │   │   ├── PluginContext.java
    │   │   │   ├── PluginManager.java
    │   │   │   ├── PluginMetadata.java
    │   │   │   ├── PluginPanel.java
    │   │   │   ├── ThemeManager.java
    │   │   │   ├── LocalizationManager.java
    │   │   │   └── impl/                         # Implementações padrão
    │   │   └── plugins/                          # Plugins integrados
    │   │       ├── counter/                      # Contador de código
    │   │       ├── formatter/                    # Formatador de código
    │   │       └── renamer/                      # Renomeação de pacotes
    │   └── resources/
    │       ├── i18n/                             # Arquivos de localização
    │       └── icons/                            # Ícones da aplicação
    └── test/
```

---

## 🔌 Desenvolvimento de plugins

1. Implemente a interface `Plugin`:

```java
public class MyPlugin implements Plugin {
    @Override public PluginMetadata getMetadata() { ... }
    @Override public void initialize(PluginContext ctx) { ... }
    @Override public void shutdown() { ... }
    @Override public boolean isInitialized() { ... }
    @Override public PluginPanel getPluginPanel() { ... }
}
```

2. Crie uma subclasse de `PluginPanel` para a interface.
3. Registre o plugin em `PluginHostApplication`.

---

## 🛠️ Stack tecnológico

| Componente | Tecnologia |
|-----------|-----------|
| Linguagem | Java 17 |
| Framework GUI | Swing |
| Look & Feel | FlatLaf 3.2 + IntelliJ Themes |
| Ícones | Ikonli (FontAwesome 5) |
| Gráficos | XChart 3.8.4 |
| Exportação Excel | Apache POI 5.2.5 |
| Exportação PDF | Apache PDFBox 2.0.31 |
| Build | Maven |

---

## 📄 Licença

Licenciado sob a **Apache License 2.0** — consulte o arquivo [LICENSE](LICENSE) para mais detalhes.

```
Copyright 2026 Spark Wan

Licenciado sob a Licença Apache, Versão 2.0 (a "Licença");
você não pode usar este arquivo exceto em conformidade com a Licença.
Você pode obter uma cópia da Licença em

    http://www.apache.org/licenses/LICENSE-2.0
```

---

## 🤝 Contribuições

Contribuições são bem-vindas! Sinta-se à vontade para enviar um Pull Request.

1. Faça um fork do repositório
2. Crie sua branch de funcionalidade (`git checkout -b feature/my-feature`)
3. Faça commit das alterações (`git commit -m 'Adicionar funcionalidade'`)
4. Faça push para a branch (`git push origin feature/my-feature`)
5. Abra um Pull Request

