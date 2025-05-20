$(document).ready(function() { // Если используете jQuery, иначе замените на нативный DOMContentLoaded
    const searchPhraseInput = $('#searchPhrase'); // jQuery: document.getElementById('searchPhrase');
    const searchOperatorSelect = $('#searchOperator'); // jQuery: document.getElementById('searchOperator');
    const searchButton = $('#searchButton'); // jQuery: document.getElementById('searchButton');
    const resultsDiv = $('#searchResults'); // jQuery: document.getElementById('searchResults');
    const loadingIndicator = $('#loadingIndicator'); // jQuery: document.getElementById('loadingIndicator');
    const resultCountSpan = $('#resultCount'); // jQuery: document.getElementById('resultCount');

    const paginationControls = $('#paginationControls'); // jQuery: document.getElementById('paginationControls');
    const prevPageButton = $('#prevPageButton'); // jQuery: document.getElementById('prevPageButton');
    const nextPageButton = $('#nextPageButton'); // jQuery: document.getElementById('nextPageButton');
    const currentPageSpan = $('#currentPage'); // jQuery: document.getElementById('currentPage');

    let currentPage = 0;
    const defaultPageSize = 10; // Соответствует size в SearchController, если не передан с фронта

    function performSearch(page = 0) {
        const phrase = searchPhraseInput.val().trim(); // jQuery: searchPhraseInput.value.trim();
        const operator = searchOperatorSelect.val(); // jQuery: searchOperatorSelect.value;

        if (!phrase) {
            // Можно показать сообщение пользователю, что поле пустое
            resultsDiv.html('<p class="text-center text-gray-500">Пожалуйста, введите фразу для поиска.</p>');
            resultCountSpan.text("Найдено: 0");
            paginationControls.hide(); // jQuery: paginationControls.style.display = 'none';
            return;
        }

        loadingIndicator.show(); // jQuery: loadingIndicator.style.display = 'block'; (или 'flex' в зависимости от CSS)
        resultsDiv.empty(); // jQuery: resultsDiv.innerHTML = '';
        paginationControls.hide(); // jQuery: paginationControls.style.display = 'none';
        resultCountSpan.text("Идет поиск...");

        const searchUrl = `/api/search/advanced?phrase=${encodeURIComponent(phrase)}&operator=${operator}&page=${page}&size=${defaultPageSize}`;

        fetch(searchUrl)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Ошибка сети или сервера: ${response.status} ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                loadingIndicator.hide();
                if (data && data.content) {
                    displayResults(data.content, phrase);
                    updatePagination(data);
                    resultCountSpan.text(`Найдено: ${data.totalElements}`);
                    if (data.totalElements === 0 && phrase) {
                        resultsDiv.html('<p class="text-center text-gray-500">По вашему запросу ничего не найдено.</p>');
                    }
                } else {
                    resultsDiv.html('<p class="text-center text-red-500">Получены некорректные данные от сервера.</p>');
                    resultCountSpan.text("Ошибка");

                }
            })
            .catch(error => {
                loadingIndicator.hide();
                resultsDiv.html(`<p class="text-center text-red-500">Ошибка при выполнении поиска: ${error.message}</p>`);
                resultCountSpan.text("Ошибка");
                console.error("Ошибка AJAX:", error);
            });
    }

    function displayResults(results, phraseToHighlight) {
        if (!results || results.length === 0) {
            // Сообщение о "ничего не найдено" обрабатывается в вызывающей функции
            return;
        }

        results.forEach(item => {
            const highlightedSentence = highlightPhrase(escapeHtml(item.sentence || ''), phraseToHighlight);
            const resultItemHtml = `
            <div class="result-item-card">
                <h3>${escapeHtml(item.title || 'Без названия')}</h3>
                <p>${highlightedSentence}</p>
                <p class="file-path-info">Файл: ${escapeHtml(item.filePath || 'Не указан')}</p>
            </div>
        `;
                                resultsDiv.append(resultItemHtml); // jQuery: resultsDiv.insertAdjacentHTML('beforeend', resultItemHtml);
                            });
                        }

    function updatePagination(pageData) {
        currentPage = pageData.pageNumber;
        currentPageSpan.text(`Страница: ${currentPage + 1} из ${pageData.totalPages}`); // jQuery

        if (pageData.totalPages > 1) {
            paginationControls.show(); // jQuery
        } else {
            paginationControls.hide(); // jQuery
        }

        prevPageButton.prop('disabled', pageData.first); // jQuery: prevPageButton.disabled = pageData.first;
        nextPageButton.prop('disabled', pageData.last);   // jQuery: nextPageButton.disabled = pageData.last;
    }

    function escapeHtml(unsafe) {
        if (typeof unsafe !== 'string') return '';
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function highlightPhrase(text, phrase) {
        if (!phrase || !text) return text;
        // Простое выделение всей фразы, если она есть.
        // Для более сложного выделения отдельных слов из фразы потребуется разбор фразы
        const phrasesToHighlight = phrase.toLowerCase().split(/\s+/).filter(p => p.length > 0); // Разбиваем фразу на слова
        let tempText = text;
        phrasesToHighlight.forEach(p => {
            const regex = new RegExp(`(${escapeRegExp(p)})`, 'gi');
            tempText = tempText.replace(regex, '<em>$1</em>');
        });
        return tempText;
    }

    function escapeRegExp(string) {
        return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }

    // Обработчики событий
    searchButton.on('click', function() { // jQuery: searchButton.addEventListener('click', ...)
        performSearch(0);
    });

    searchPhraseInput.on('keypress', function(e) { // jQuery
        if (e.which === 13 || e.keyCode === 13) { // Enter key pressed
            e.preventDefault(); // Предотвратить отправку формы, если инпут в форме
            performSearch(0);
        }
    });

    prevPageButton.on('click', function() { // jQuery
        if (currentPage > 0) {
            performSearch(currentPage - 1);
        }
    });

    nextPageButton.on('click', function() { // jQuery
        // Проверка на pageData.last уже есть в updatePagination, но можно добавить и здесь
        performSearch(currentPage + 1);
    });

    // jQuery specific hide/show/empty/val/prop/on. If not using jQuery, replace with vanilla JS:
    // .show() -> .style.display = 'block' or 'flex', etc.
    // .hide() -> .style.display = 'none'
    // .empty() -> .innerHTML = ''
    // .val() -> .value
    // .prop('disabled', true/false) -> .disabled = true/false
    // .on('click', ...) -> .addEventListener('click', ...)
    // .append(html) -> .insertAdjacentHTML('beforeend', html)
    // .html(html) -> .innerHTML = html
    // .text(text) -> .textContent = text

    // Первоначальное скрытие пагинации и индикатора (на случай если они видимы в HTML)
    loadingIndicator.hide();
    paginationControls.hide();

}); // Конец $(document).ready() или DOMContentLoaded
