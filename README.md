# CodeSentinel AI

> AI-powered code reviews, directly inside GitHub Pull Requests.

CodeSentinel AI is an AI-powered code review platform that analyzes GitHub Pull Requests, identifies potential bugs, security vulnerabilities, performance issues, and code-quality problems, and provides contextual recommendations directly on the Pull Request.

---

## ✨ Features

- 🤖 AI-powered Pull Request analysis
- 🔐 Security vulnerability detection
- 🐛 Bug and reliability detection
- ⚡ Performance issue detection
- 💬 Contextual inline GitHub review comments
- 📝 AI-generated Pull Request summaries
- 📊 Review history and findings
- ⚙️ Configurable review rules
- 🔄 Automatic reviews when Pull Requests are opened or updated
- 🎯 High-confidence actionable findings
- 🧠 Context-aware code analysis

---

## 🏗️ Architecture

```text
                         ┌─────────────────────┐
                         │      Developer      │
                         └──────────┬──────────┘
                                    │
                                    │ Creates / Updates PR
                                    ▼
                         ┌─────────────────────┐
                         │       GitHub        │
                         │   Pull Request     │
                         └──────────┬──────────┘
                                    │
                                    │ Webhook
                                    ▼
                         ┌─────────────────────┐
                         │   CodeSentinel AI   │
                         │        API          │
                         └──────────┬──────────┘
                                    │
                                    │ Enqueue Review
                                    ▼
                         ┌─────────────────────┐
                         │    Redis / BullMQ   │
                         │      Queue          │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │    AI Review        │
                         │      Worker         │
                         └──────────┬──────────┘
                                    │
                 ┌──────────────────┼──────────────────┐
                 │                  │                  │
                 ▼                  ▼                  ▼
        ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
        │    Security    │ │      Bugs      │ │  Performance   │
        │    Analysis    │ │    Analysis    │ │    Analysis    │
        └────────────────┘ └────────────────┘ └────────────────┘
                 │                  │                  │
                 └──────────────────┼──────────────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │   Review Findings   │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │    GitHub Review    │
                         │     Comments        │
                         └─────────────────────┘
