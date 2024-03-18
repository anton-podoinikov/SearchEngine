# "Поисковый движок"

Приложение представляет собой поисковую систему, предназначенную для индексации, поиска и предоставления статистики по веб-страницам. Оно состоит из трех основных сервисов, каждый из которых выполняет свою роль в обеспечении функциональности системы:

__Сервис индексирования (IndexingService)__: Отвечает за процесс индексирования веб-страниц. Этот сервис сканирует указанные сайты, анализирует их содержимое, извлекает и обрабатывает данные (тексты страниц, ссылки и т.д.), сохраняя полученную информацию в базе данных. Он позволяет начать индексирование всего списка сайтов или отдельных URL-адресов, а также остановить процесс индексирования.

__Сервис поиска (SearchService)__: Предоставляет возможность поиска по индексированным данным на основе пользовательских запросов. Сервис обрабатывает поисковые запросы, анализирует индексированные данные, используя лемматизацию и другие методы обработки текста, для нахождения наиболее релевантных результатов поиска. Результаты включают в себя информацию о страницах, такую как URL, заголовок, сниппет с выделением искомых слов, а также метрику релевантности.

__Сервис статистики (StatisticsService)__  : Собирает и предоставляет статистические данные о состоянии индексации сайтов. Сервис отображает общую информацию по всем сайтам и детализированную статистику по каждому индексированному сайту, включая количество страниц, количество уникальных лемм, статус индексации, возможные ошибки и время последнего обновления статуса.

В целом, приложение обеспечивает комплексный подход к организации поиска информации в интернете, начиная от сбора данных с веб-страниц и заканчивая предоставлением пользователю актуальных и релевантных результатов поиска, а также статистики по индексированным ресурсам.

___

## Стек технологий примененный в приложении:
+ Spring Boot для создания веб-сервиса
+ Jsoup для парсинга HTML-страниц
+ Lombok для упрощения создания моделей и сервисов
+ MySQL для хранения данных
___
## Краткое описание ключевых сервисов:
### _1. Описание сервиса IndexingService:_

Сервис `IndexingService` предназначен для индексирования веб-страниц. Он позволяет начать процесс индексирования для списка сайтов, указанных в конфигурации, индексировать отдельные URL, а также остановить текущий процесс индексирования.

### Основные компоненты
+ `SitesList sites`: список сайтов для индексирования.
+ `PageRepository pageRepository`: репозиторий для работы со страницами.
+ `SiteRepository siteRepository`: репозиторий для работы с сайтами.
+ `LemmaRepository lemmaRepository`: репозиторий для работы с леммами.
+ `IndexRepository indexRepository`: репозиторий для работы с индексами.
+ `LemmaFinderImpl lemmaFinder`: компонент для нахождения и обработки лемм на странице.
+ `List<Thread> indexingThreads`: список потоков, используемых для индексации.
+ `ForkJoinPool forkJoinPool`: пул потоков для параллельной обработки задач.
### Методы сервиса
+ `startIndexing()`: начинает процесс индексирования для всех сайтов из списка. Если индексирование уже выполняется, возвращает соответствующее сообщение.
+ `startIndexingUrl(String url)`: начинает индексирование для конкретного URL. Если URL невалидный или индексирование уже выполняется, возвращает ошибку.
+ `stopIndexing()`: останавливает текущий процесс индексирования, прерывая все активные потоки.
### Процесс индексирования
Индексирование сайта включает в себя парсинг HTML-контента страниц, извлечение и обработку ссылок на другие страницы, обработку текста страниц для нахождения лемм и сохранение информации о страницах, леммах и индексах в соответствующие репозитории.

Для каждой страницы создается запись в базе данных с информацией о содержимом страницы, ее URL и статусе ответа. Для текста страницы выполняется лемматизация, на основе которой формируются индексные записи, связывающие страницы с найденными леммами и их частотой в тексте.

### Особенности реализации
+ Синхронизация потоков обеспечивается с помощью объекта lock для предотвращения одновременного запуска нескольких процессов индексирования.
+ Для параллельной обработки используется ForkJoinPool, размер которого определяется количеством доступных процессоров.
+ При индексировании отдельного URL предварительно проверяется его валидность и наличие в базе данных. Если страница уже существует, связанные с ней данные удаляются перед новым индексированием.
### Управление данными
+ Перед началом индексирования производится очистка данных, связанных с уже индексированными страницами, для предотвращения дублирования информации.
+ Для сохранения данных используются репозитории Spring Data, что обеспечивает абстракцию от конкретной реализации базы данных и упрощает работу с данными.


Этот сервис является ключевым компонентом системы поискового индекса, обеспечивая сбор, обработку и хранение данных, необходимых для функционирования поисковой системы.

### _2. Описание сервиса SearchService:_

Сервис `SearchService` предоставляет функционал для поиска по веб-страницам, индексированным в базе данных, на основе введенного пользовательского запроса. Поиск может выполняться как по всем сайтам, так и по конкретному сайту. Сервис возвращает результаты с учетом релевантности страниц запросу, включая сниппеты (фрагменты текста) с выделением искомых слов.

### Основные компоненты
+ `LemmaFinder lemmaFinder`: компонент для нахождения и обработки лемм в запросе.
+ `LemmaRepository lemmaRepository`: репозиторий для работы с леммами.
+ `PageRepository pageRepository`: репозиторий для работы со страницами.
+ `SiteRepository siteRepository`: репозиторий для работы с сайтами.
### Методы сервиса
+ `findByLemmaInDatabase(String query, int offset, int limit)`: осуществляет поиск по базе данных на основе запроса без учета конкретного сайта.
+ `findByLemmaInDatabase(String query, String site, int offset, int limit)`: осуществляет поиск по базе данных на основе запроса с учетом конкретного сайта.
### Процесс поиска
1. __Лемматизация запроса__: Преобразование запроса пользователя в набор лемм с помощью LemmaFinder.
1. __Фильтрация лемм__: Отбор лемм, частота которых не превышает заданный порог.
1. __Поиск релевантных__ страниц: Определение страниц, содержащих все леммы из запроса.
1. __Фильтрация по близости слов__: Отбор страниц, на которых искомые слова расположены на максимально допустимом расстоянии друг от друга.
1. __Расчет релевантности__: Определение релевантности страниц запросу на основе количества вхождений лемм и их взвешивания.
1. __Сортировка результатов__: Ранжирование найденных страниц по убыванию релевантности.
1. __Формирование сниппетов__: Генерация коротких фрагментов текста из содержимого страницы, содержащих искомые слова с выделением их жирным шрифтом.
1. __Пагинация результатов__: Возврат страницы результатов в соответствии с заданными параметрами offset и limit.
### Особенности реализации
+ Поиск может выполняться как по всем сайтам, так и по указанному сайту. В последнем случае сначала проверяется, готов ли индекс для этого сайта.
+ Для фильтрации используется пороговое значение частоты лемм, чтобы исключить слишком общие слова.
+ Релевантность страницы определяется на основе абсолютной релевантности, нормализованной относительно страницы с наивысшей релевантностью.
+ Сниппеты создаются путем выбора фрагментов текста вокруг искомых слов с дальнейшим выделением этих слов в тексте.
+ Используется Jsoup для обработки HTML-контента страниц и извлечения текстовой информации.
### Управление данными
+ Данные о страницах, леммах и сайтах хранятся в соответствующих репозиториях и используются для выполнения поисковых запросов.
+ Результаты поиска включают в себя URL страницы, заголовок, сниппет с выделением искомых слов, релевантность страницы запросу, а также URL и название сайта.
+ Сервис поиска является ключевым компонентом системы поискового индекса, позволяя пользователям находить наиболее релевантную информацию в индексированном контенте.

### _3. Описание сервиса StatisticsService:_

Сервис `StatisticsService` предназначен для сбора и предоставления статистических данных о состоянии и содержании индексируемых сайтов. Сервис собирает общую и детализированную статистику по каждому сайту, включая количество страниц, лемм, статус индексации и возможные ошибки.

### Основные компоненты и зависимости
`PageRepository pageRepository`: используется для подсчета количества страниц, индексированных для каждого сайта.
`LemmaRepository lemmaRepository`: применяется для определения количества уникальных лемм, ассоциированных с каждым сайтом.
`SiteRepository siteRepository`: служит для получения списка всех сайтов и извлечения информации о них, включая статус и время последнего обновления статуса.
### Функциональность
 Получение статистики: Метод getStatistics() возвращает статистические данные по всем сайтам, которые есть в базе данных. Данные включают общую статистику по всем сайтам и детализированную информацию по каждому сайту отдельно.
+ Общая статистика (TotalStatistics) включает:
  + Флаг индексации, указывающий на то, происходит ли в данный момент процесс индексации.
  + Общее количество сайтов.
  + Суммарное количество страниц и лемм по всем сайтам.
+ Детализированная статистика (DetailedStatisticsItem) для каждого сайта включает:
  + Название сайта.
  + URL сайта.
  + Количество страниц, индексированных на сайте.
  + Количество уникальных лемм, найденных на сайте.
  + Статус индексации сайта.
  + Информацию об ошибках, если таковые имелись во время последней индексации.
  + Время последнего обновления статуса сайта.
### Пример использования
Сервис может быть использован веб-интерфейсом поисковой системы или другими сервисами для отображения актуальной статистики по индексированным сайтам. Это может включать в себя административный интерфейс для мониторинга состояния индексации и оперативного реагирования на возможные ошибки или проблемы.

### Реализация
+ В методе `getStatistics()` сначала извлекается список всех сайтов через `SiteRepository`.
+ Для каждого сайта подсчитывается количество страниц и лемм, используя методы `countBySiteId` из соответствующих репозиториев.
+ Собирается детализированная статистика по каждому сайту и обновляется общая статистика.
+ Статистические данные упаковываются в объект `StatisticsResponse` и возвращаются вызывающей стороне.
### Вывод
Сервис статистики `StatisticsService` играет ключевую роль в обеспечении прозрачности и контроля за процессом индексации сайтов, позволяя администраторам и пользователям получать актуальную информацию о состоянии поискового индекса и его компонентах.
___

## Запуск приложения

Для локального запуска приложения поисковой системы на основе Spring Boot и Maven как систему сборки, вы можете следовать следующим шагам. Эта инструкция предполагает, что у вас уже установлены JDK (Java Development Kit) и Maven.

Если у вас не установлена база данных MySql, обратитесь к пункту "Дополнительно".

### Шаг 1: Подготовка окружения
Убедитесь, что на вашем компьютере установлены JDK и Maven:

+ JDK: Версия 17 или выше. Проверить установку и версию JDK можно с помощью команды java -version в командной строке или терминале.
+ Maven: Проверить установку и версию Maven можно с помощью команды mvn -version.

### Шаг 2: Клонирование репозитория
Клонируйте код вашего приложения из Git репозитория командой:

```
git clone <https://github.com/anton-podoinikov/SearchEngine.git>
```

### Шаг 3: Сборка приложения
Перейдите в директорию проекта:

```
cd <название_директории_проекта>
```

Запустите сборку проекта с помощью Maven:

```
mvn clean install
```

Эта команда скачает необходимые зависимости, выполнит компиляцию и сборку проекта.

### Шаг 4: Запуск приложения
После успешной сборки запустите приложение с помощью Spring Boot Maven plugin:

```
mvn spring-boot:run
```

Приложение запустится и будет доступно локально. По умолчанию, Spring Boot приложения запускаются на порту 8080, так что вы сможете обратиться к нему через браузер или API клиент по адресу http://localhost:8080.

Для остановки приложения нажмите в командной строке Ctrl+C.

## Дополнительно
Для запуска так же потребуется локально установленная база данных MySql.

### Шаг 1: Установка Docker
Если Docker еще не установлен на вашей машине, сначала установите его. Инструкции по установке для разных операционных систем доступны на официальном сайте Docker.

### Шаг 2: Запуск контейнера MySQL
1. Запуск контейнера MySQL. Вы можете запустить новый контейнер MySQL, используя следующую команду в терминале. Если образ MySQL еще не загружен на вашу машину, Docker автоматически скачает его из Docker Hub.

```
docker run --name mysql-container -e MYSQL_ROOT_PASSWORD=your_password -p 3306:3306 -d mysql:latest
```
* `--name mysql-container` задает имя контейнера как mysql-container.
* `-e MYSQL_ROOT_PASSWORD=your_password` устанавливает пароль для пользователя root. Замените your_password на ваш собственный пароль.
* `-p 3306:3306` пробрасывает порт 3306 контейнера на локальный порт 3306.
* `-d mysql:latest` указывает Docker использовать последнюю версию образа MySQL.

Теперь MySQL доступен на порту 3306 вашей хост-машины.

Проверьте, что контейнер запущен, выполнив:
```
docker ps
```

### Шаг 3: Подключение к MySQL
После запуска контейнера вы можете подключиться к MySQL, используя любой клиент MySQL, например, командную строку или графический интерфейс пользователя (GUI) такой как MySQL Workbench.

Или введите в командной строке:

```
docker exec -it mysql-container mysql -uroot -p
```

Вам будет предложено ввести пароль, который вы указали при запуске контейнера `(your_password)`.

### Шаг 4: Управление контейнером
* Остановка контейнера:
```
docker stop mysql-container
```

* Запуск остановленного контейнера:
```
docker start mysql-container
```

* Удаление контейнера (после остановки):
```
docker rm mysql-container
```

Эти шаги помогут вам настроить и запустить MySQL в Docker контейнере, а также управлять этим контейнером. Docker облегчает развертывание баз данных и других сервисов, делая их изоляцией и конфигурированием более простыми и удобными.

### Шаг 5: Настройка конфигурационного файла приложения

Завершающим этапом настройки будет внесение изменений логина и пароля для доступа к базе данных в конфигурационный файл.

Перейдите в папку приложения, которую вы клонировали с удаленного репозитория на свой ПК. Найдите файл конфигурации application.yaml и откройте его с помощью текстового редактора.

Измените параметры подключения к базе данных (username и password) на значения, указанные при создании контейнера MySQL.

```yaml
spring:
  datasource:
    username: root
    password: your_password
```

После этих шагов, пробуйте запустить приложение.

