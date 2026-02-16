# Prompt: Aplicar opção A – commits sem Co-authored-by

Copie o texto abaixo e use no Cursor quando quiser que os commits sigam a opção A (sem linha Co-authored-by e com mensagem em inglês descrevendo a funcionalidade).

---

## Texto para colar no Cursor

```
Apply "option A" for my commits in this repo:

1. **Do not add** "Co-authored-by: Cursor <cursoragent@cursor.com>" to any commit message.

2. **Commit messages** must be in English and describe the functionality (what was added/changed and why), not implementation details only.

3. **When you create or amend a commit:**
   - Use an explicit message (e.g. `-m "..."` or `-F arquivo.txt`) and pass `--no-verify` so that no hook adds the Co-authored-by line.
   - Or, if amending an existing commit that already has Co-authored-by, replace it by creating a new commit with the same tree and parent using `git commit-tree`, then `git update-ref refs/heads/<branch> <new-commit-hash>`, so the message is exactly what we want with no Co-authored-by.

4. **Preferred flow:** When I ask you to commit, suggest or use a short English message that describes the feature/fix (e.g. "Add X" / "Fix Y") and run `git commit` with `--no-verify` and that message so the Co-authored-by line is never added.
```

---

## Resumo da opção A

| Item | Regra |
|------|--------|
| Co-authored-by | Nunca incluir nos commits. |
| Mensagem | Sempre em inglês, descrevendo a funcionalidade. |
| Como commitar | Usar mensagem explícita + `--no-verify`, ou `commit-tree` ao remover Co-authored-by de um commit já feito. |

Use este prompt sempre que quiser que o Cursor siga esse padrão de commit.
