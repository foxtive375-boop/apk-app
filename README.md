# Domino Fever Offline (Android, Kotlin)

Полностью офлайн Android-приложение на Kotlin (Android Studio) с архитектурой MVVM.

## Реализовано

- Режимы: `Draw Dominoes`, `Block Dominoes`, `All Fives`, `All Threes`, `Cross / Multi-branch`
- `Quick Play`
- `Tournament Mode` (карьера по этапам с локациями, ростом сложности ИИ, рейтингом и финальным турниром)
- Стандартный набор домино `0..6`
- Раздача по 7 костей
- Настраиваемый старт: с дубля / с минимальной кости
- Добор из базара (в draw-режимах) до возможности хода
- Пасс в block-режиме и при пустом базаре
- Подсчёт очков отдельными классами для каждого режима
- Разветвления после дублей в `Cross`
- ИИ с уровнями сложности, псевдо-памятью и эвристиками риска/блефа
- Кастомный `Canvas`-стол с отрисовкой цепочки, веток, концов и анимацией выкладывания
- Подсветка доступных ходов в руке

## Технически

- Kotlin + Android View system
- MVVM (`GameViewModel` + immutable UI state)
- Логика игры отделена от UI (`engine`, `scoring`, `ai`)
- Полностью офлайн, без сетевых зависимостей

## Сборка локально

```bash
./gradlew :app:assembleDebug
```

APK:

`app/build/outputs/apk/debug/app-debug.apk`

## CI

GitHub Actions workflow: `.github/workflows/android.yml`

- Запускает `:app:testDebugUnitTest`
- Собирает `:app:assembleDebug`
- Публикует APK как artifact
