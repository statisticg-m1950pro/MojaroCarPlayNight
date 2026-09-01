MONJARO CARPLAY NIGHT V4 ROOT

Назначение
==========
V4 предназначена для GMC с уже имеющимся root-доступом.

Что реально делает
==================
1. Проверяет наличие su.
2. Запускает root-демон /data/local/tmp/monjaro_rootd.sh.
3. Демон раз в секунду читает:
   settings get system realThemeDaynightMode
4. При смене значения пишет состояние в:
   /data/local/tmp/monjaro_carplay_state
5. Ведёт журнал:
   /data/local/tmp/monjaro_carplay_root.log
6. Создаёт hook-кандидаты:
   /data/local/tmp/night_mode
   /tmp/night_mode
7. Оставляет V3 foreground/watchdog как резерв.

Важно
=====
Команды StartNightMode/StopNightMode относятся к USB-протоколу CPC200.
Сам факт root НЕ означает, что запись в /tmp/night_mode автоматически будет
прочитана AutoKit. Поэтому V4 честно разделяет две части:
- ROOT-автоматизация датчика света — реализована;
- ROOT-hook AutoKit — реализован как диагностический кандидат и требует
  подтверждения на конкретной прошивке GMC/AutoKit.

После установки
===============
1. Открыть приложение.
2. Нажать «Активировать ROOT-движок».
3. Разрешить su в менеджере root.
4. Нажать «ROOT диагностика AutoKit».
5. Проверить ТЕСТ НОЧЬ/ДЕНЬ.
6. После перезагрузки снова открыть V4 один раз только для проверки статуса.

GitHub Actions
==============
Workflow уже лежит в .github/workflows/build-apk.yml.
