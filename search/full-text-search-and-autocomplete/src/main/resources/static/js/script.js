// Variables to store the current search state
let currentTitle = '';
let currentExtract = '';
let currentCast = [];
let currentYear = null;
let currentGenres = [];
let selectedAutocompleteIndex = -1;
let allGenres = []; // Store all available genres
let filteredGenres = []; // Store filtered genres based on search
let selectedGenreIndex = -1; // Track selected genre in dropdown
let allActors = []; // Store all available actors
let filteredActors = []; // Store filtered actors based on search
let selectedCastIndex = -1; // Track selected actor in dropdown

// Debounce function to limit how often a function can be called
function debounce(func, wait) {
    let timeout;
    return function(...args) {
        const context = this;
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(context, args), wait);
    };
}

// Function to fetch autocomplete suggestions
function fetchAutocompleteSuggestions(query) {
    if (!query || query.trim() === '') {
        hideAutocompleteResults();
        return Promise.resolve({ suggestions: [], autocompleteTime: 0 });
    }

    console.log(`Fetching autocomplete suggestions for: ${query}`);
    return fetch(`/search/${encodeURIComponent(query)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Autocomplete suggestions:', data);
            return data;
        })
        .catch(error => {
            console.error('Error fetching autocomplete suggestions:', error);
            return { suggestions: [], autocompleteTime: 0 };
        });
}

// Function to display autocomplete suggestions
function displayAutocompleteSuggestions(data) {
    const autocompleteResults = document.getElementById('autocomplete-results');
    const suggestions = data.suggestions || [];
    const autocompleteTime = data.autocompleteTime || 0;

    if (!suggestions || suggestions.length === 0) {
        hideAutocompleteResults();
        return;
    }

    // Log the first suggestion to see its structure
    if (suggestions.length > 0) {
        console.log('First suggestion structure:', suggestions[0]);
    }

    // Create a container for the timing information
    let html = `<div class="timing-info">Autocomplete time: ${autocompleteTime} ms</div>`;

    // Add suggestions
    suggestions.forEach((suggestion, index) => {
        // Try different possible property names for the suggestion text
        const suggestionText = suggestion.text || suggestion.value || suggestion.label || suggestion.suggestion || suggestion;
        // Get the payload (extract) if available
        let payloadText = '';
        if (suggestion.payload) {
            // Check if payload is an object
            if (typeof suggestion.payload === 'object' && suggestion.payload !== null) {
                // Try to get the extract property or stringify the object
                payloadText = suggestion.payload.extract || JSON.stringify(suggestion.payload);
            } else {
                payloadText = suggestion.payload;
            }
        }
        html += `<div class="autocomplete-item" data-index="${index}"><div class="suggestion-text">${suggestionText}</div>${payloadText ? `<div class="suggestion-extract">${payloadText}</div>` : ''}</div>`;
    });

    autocompleteResults.innerHTML = html;
    autocompleteResults.style.display = 'block';

    // Add click event listeners to autocomplete items
    document.querySelectorAll('.autocomplete-item').forEach(item => {
        item.addEventListener('click', function() {
            const index = parseInt(this.getAttribute('data-index'));
            selectAutocompleteSuggestion(suggestions[index]);
        });

        item.addEventListener('mouseover', function() {
            // Remove selected class from all items
            document.querySelectorAll('.autocomplete-item').forEach(i => {
                i.classList.remove('selected');
            });
            // Add selected class to this item
            this.classList.add('selected');
            selectedAutocompleteIndex = parseInt(this.getAttribute('data-index'));
        });
    });
}

// Function to hide autocomplete results
function hideAutocompleteResults() {
    const autocompleteResults = document.getElementById('autocomplete-results');
    autocompleteResults.innerHTML = '';
    autocompleteResults.style.display = 'none';
    selectedAutocompleteIndex = -1;
}

// Function to select an autocomplete suggestion
function selectAutocompleteSuggestion(suggestion) {
    const searchInput = document.getElementById('search-input');
    // Try different possible property names for the suggestion text
    const suggestionText = suggestion.text || suggestion.value || suggestion.label || suggestion.suggestion || suggestion;
    searchInput.value = suggestionText;
    hideAutocompleteResults();
    searchInput.focus();
}

// Function to search movies
function searchMovies(title, extract, cast, year, genres) {
    console.log(`Searching movies with title: ${title}, extract: ${extract}, cast: ${cast}, year: ${year}, genres: ${genres}`);

    // Build the URL with query parameters
    let url = '/search?';
    const params = [];

    if (title && title.trim() !== '') {
        params.push(`title=${encodeURIComponent(title.trim())}`);
    }

    if (extract && extract.trim() !== '') {
        params.push(`text=${encodeURIComponent(extract.trim())}`);
    }

    if (cast && cast.length > 0) {
        cast.forEach(actor => {
            params.push(`cast=${encodeURIComponent(actor.trim())}`);
        });
    }

    if (year) {
        params.push(`year=${year}`);
    }

    if (genres && genres.length > 0) {
        genres.forEach(genre => {
            params.push(`genres=${encodeURIComponent(genre.trim())}`);
        });
    }

    url += params.join('&');

    return fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Search results:', data);
            return data;
        })
        .catch(error => {
            console.error('Error searching movies:', error);
            return { movies: [], count: 0 };
        });
}

// Function to display search results
function displaySearchResults(results) {
    const resultsContainer = document.getElementById('search-results');
    const resultsCount = document.getElementById('results-count');

    if (!results || !results.movies || results.movies.length === 0) {
        resultsCount.textContent = 'No movies found';
        resultsContainer.innerHTML = '<p class="no-results">No movies match your search criteria. Try adjusting your filters.</p>';
        return;
    }

    // Display search time and results count
    const searchTime = results.searchTime || 0;
    resultsCount.textContent = `Found ${results.count} movie${results.count !== 1 ? 's' : ''} in ${searchTime} ms`;

    let html = '';
    results.movies.forEach((movie, index) => {
        html += `<div class="movie-card" data-movie-index="${index}">${movie.thumbnail ? `<img src="${movie.thumbnail}" alt="${movie.title}" class="movie-thumbnail">` : ''}<div class="movie-info"><h3 class="movie-title">${movie.title}</h3><p class="movie-year">${movie.year}</p>${movie.cast && movie.cast.length > 0 ? `<p class="movie-cast"><strong>Cast:</strong> ${movie.cast.join(', ')}</p>` : ''}${movie.genres && movie.genres.length > 0 ? `<p class="movie-genres"><strong>Genres:</strong> ${movie.genres.join(', ')}</p>` : ''}${movie.extract ? `<div class="extract-container"><p class="movie-extract" id="extract-${index}">${movie.extract}</p><span class="read-more-btn" data-target="extract-${index}">Read more</span></div>` : ''}</div></div>`;
    });

    resultsContainer.innerHTML = html;

    // Add event listeners to "Read more" buttons
    document.querySelectorAll('.read-more-btn').forEach(button => {
        button.addEventListener('click', function() {
            const targetId = this.getAttribute('data-target');
            const extractElement = document.getElementById(targetId);

            if (extractElement.classList.contains('expanded')) {
                extractElement.classList.remove('expanded');
                this.textContent = 'Read more';
            } else {
                extractElement.classList.add('expanded');
                this.textContent = 'Read less';
            }
        });
    });
}

// Function to parse comma-separated values
function parseCommaSeparatedValues(input) {
    if (!input || input.trim() === '') {
        return [];
    }

    return input.split(',')
        .map(item => item.trim())
        .filter(item => item !== '');
}

// Function to fetch all genres
function fetchAllActors() {
    console.log('Fetching all actors');
    return fetch('/actors')
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('All actors:', data);
            return data;
        })
        .catch(error => {
            console.error('Error fetching actors:', error);
            return { actors: [], count: 0 };
        });
}

// Function to fetch all genres
function fetchAllGenres() {
    console.log('Fetching all genres');
    return fetch('/genres')
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('All genres:', data);
            return data;
        })
        .catch(error => {
            console.error('Error fetching genres:', error);
            return { genres: [], count: 0 };
        });
}

// Function to display genres in the dropdown
function displayGenres(genres) {
    const genreDropdown = document.getElementById('genre-dropdown');
    console.log('Displaying genres:', genres);
    console.log('Current selected genres:', currentGenres);

    if (!genres || genres.length === 0) {
        genreDropdown.innerHTML = '<div class="genre-item">No genres found</div>';
        console.log('No genres found, setting empty dropdown');
        return;
    }

    let html = '';
    genres.forEach((genre, index) => {
        const isSelected = currentGenres.includes(genre);
        // Create a more structured HTML with explicit labels and better semantics
        html += `
            <div class="genre-item ${isSelected ? 'selected' : ''}" data-genre="${genre}" data-index="${index}">
                <label class="genre-label">
                    <input type="checkbox" class="genre-checkbox" ${isSelected ? 'checked' : ''} id="genre-checkbox-${index}">
                    <span class="genre-text">${genre}</span>
                </label>
            </div>
        `;
    });

    // Remove any whitespace/newlines from the HTML to avoid layout issues
    html = html.replace(/\s+/g, ' ').trim();

    console.log('Generated HTML for genre dropdown:', html);
    genreDropdown.innerHTML = html;

    // Add click event listeners to genre items
    document.querySelectorAll('.genre-item').forEach(item => {
        // Get the checkbox, label, and text elements
        const checkbox = item.querySelector('.genre-checkbox');
        const label = item.querySelector('.genre-label');
        const text = item.querySelector('.genre-text');

        // We don't need to add a click handler to the item itself anymore
        // since the label will handle the clicks and toggle the checkbox

        // Add change event to the checkbox
        checkbox.addEventListener('change', function(e) {
            console.log('Checkbox change event:', this);
            console.log('Event target:', e.target);
            console.log('Current checkbox state:', this.checked);

            // No need to stop propagation as the label's for attribute will handle this

            const genre = item.getAttribute('data-genre');
            console.log('Genre from checkbox change:', genre);

            if (this.checked) {
                // Add genre to selected genres
                item.classList.add('selected');
                if (!currentGenres.includes(genre)) {
                    currentGenres.push(genre);
                }
            } else {
                // Remove genre from selected genres
                item.classList.remove('selected');
                currentGenres = currentGenres.filter(g => g !== genre);
            }

            updateSelectedGenresDisplay();
            console.log('Selected genres after checkbox change:', currentGenres);
        });

        // Log when the item is clicked for debugging
        item.addEventListener('click', function(e) {
            console.log('Genre item clicked:', this);
            console.log('Event target:', e.target);
            console.log('Is checkbox:', e.target === checkbox);
            console.log('Is label:', e.target === label);
            console.log('Is text:', e.target === text);

            // We don't need to handle the click here as the label will do it
            // Just log for debugging
        });
    });
}

// Function to filter genres based on search input
function filterGenres(searchText) {
    if (!searchText || searchText.trim() === '') {
        filteredGenres = [...allGenres];
    } else {
        const searchLower = searchText.toLowerCase();
        filteredGenres = allGenres.filter(genre => 
            genre.toLowerCase().includes(searchLower)
        );
    }

    displayGenres(filteredGenres);
}

// Function to update the display of selected genres
function updateSelectedGenresDisplay() {
    const genreSearchContainer = document.getElementById('genre-search-container');

    // Remove existing selected genres display if it exists
    const existingDisplay = document.querySelector('.selected-genres');
    if (existingDisplay) {
        existingDisplay.remove();
    }

    if (currentGenres.length === 0) {
        return;
    }

    // Create new selected genres display
    const selectedGenresDisplay = document.createElement('div');
    selectedGenresDisplay.className = 'selected-genres';

    currentGenres.forEach(genre => {
        const genreElement = document.createElement('div');
        genreElement.className = 'selected-genre';
        genreElement.innerHTML = `${genre}<span class="remove-genre" data-genre="${genre}">&times;</span>`;
        selectedGenresDisplay.appendChild(genreElement);
    });

    // Insert after the genre search input
    genreSearchContainer.insertAdjacentElement('afterend', selectedGenresDisplay);

    // Add event listeners to remove buttons
    document.querySelectorAll('.remove-genre').forEach(button => {
        button.addEventListener('click', function() {
            const genre = this.getAttribute('data-genre');
            currentGenres = currentGenres.filter(g => g !== genre);
            updateSelectedGenresDisplay();

            // Update the checkboxes in the dropdown
            const genreItem = document.querySelector(`.genre-item[data-genre="${genre}"]`);
            if (genreItem) {
                const checkbox = genreItem.querySelector('.genre-checkbox');
                checkbox.checked = false;
                genreItem.classList.remove('selected');
            }
        });
    });

    // Update the hidden select element
    updateGenreSelect();
}

// Function to update the hidden select element with selected genres
function updateGenreSelect() {
    const genreSelect = document.getElementById('genre-filter');

    // Clear existing options
    genreSelect.innerHTML = '';

    // Add options for selected genres
    currentGenres.forEach(genre => {
        const option = document.createElement('option');
        option.value = genre;
        option.textContent = genre;
        option.selected = true;
        genreSelect.appendChild(option);
    });
}

// Initialize the page when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('search-input');
    const searchForm = document.getElementById('search-form');
    const yearFilter = document.getElementById('year-filter');
    const castFilter = document.getElementById('cast-filter');
    const genreFilter = document.getElementById('genre-filter');
    const genreSearch = document.getElementById('genre-search');
    const genreDropdown = document.getElementById('genre-dropdown');
    const genreToggleBtn = document.getElementById('genre-toggle-btn');

    // Initialize the toggle button state
    if (genreDropdown.style.display === 'block') {
        genreToggleBtn.classList.add('active');
    } else {
        genreToggleBtn.classList.remove('active');
    }

    console.log('Initial genre dropdown display:', genreDropdown.style.display);
    console.log('Initial genre toggle button state:', genreToggleBtn.classList.contains('active'));

    // Set up autocomplete with debounce
    const debouncedAutocomplete = debounce(function(query) {
        fetchAutocompleteSuggestions(query)
            .then(data => {
                displayAutocompleteSuggestions(data);
            });
    }, 300);

    searchInput.addEventListener('input', function() {
        currentTitle = this.value;
        debouncedAutocomplete(currentTitle);
    });

    // Handle keyboard navigation for autocomplete
    searchInput.addEventListener('keydown', function(e) {
        const autocompleteItems = document.querySelectorAll('.autocomplete-item');

        if (autocompleteItems.length === 0) {
            return;
        }

        // Down arrow
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            selectedAutocompleteIndex = (selectedAutocompleteIndex + 1) % autocompleteItems.length;
            updateSelectedAutocompleteItem();
        }
        // Up arrow
        else if (e.key === 'ArrowUp') {
            e.preventDefault();
            selectedAutocompleteIndex = selectedAutocompleteIndex <= 0 ? autocompleteItems.length - 1 : selectedAutocompleteIndex - 1;
            updateSelectedAutocompleteItem();
        }
        // Enter key
        else if (e.key === 'Enter' && selectedAutocompleteIndex >= 0) {
            e.preventDefault();
            const suggestions = document.querySelectorAll('.autocomplete-item');
            if (suggestions[selectedAutocompleteIndex]) {
                suggestions[selectedAutocompleteIndex].click();
            }
        }
        // Escape key
        else if (e.key === 'Escape') {
            hideAutocompleteResults();
        }
    });

    // Hide autocomplete results when clicking outside
    document.addEventListener('click', function(e) {
        if (!e.target.closest('.search-input-container')) {
            hideAutocompleteResults();
        }
    });

    // Fetch and populate genres dropdown
    fetchAllGenres()
        .then(data => {
            allGenres = data.genres || [];
            filteredGenres = [...allGenres];
            displayGenres(filteredGenres);

            // Show the dropdown initially to make it easier to select options
            setTimeout(() => {
                // Explicitly set the display style to block
                genreDropdown.style.display = 'block';
                // Update the toggle button state
                document.getElementById('genre-toggle-btn').classList.add('active');
                console.log('Genre dropdown displayed initially');

                // Add a message to help users understand how to use the dropdown
                if (genreDropdown.firstChild) {
                    const helpMessage = document.createElement('div');
                    helpMessage.className = 'genre-help-message';
                    helpMessage.textContent = 'Click on a genre to select it';
                    genreDropdown.insertBefore(helpMessage, genreDropdown.firstChild);
                }

                // Force a reflow to ensure the dropdown is visible
                genreDropdown.offsetHeight;
            }, 500); // Short delay to ensure the DOM is ready
        });

    // Set up genre search functionality
    genreSearch.addEventListener('input', function() {
        filterGenres(this.value);
    });

    // Show/hide genre dropdown on focus
    genreSearch.addEventListener('focus', function() {
        genreDropdown.style.display = 'block';
        document.getElementById('genre-toggle-btn').classList.add('active');
        console.log('Genre dropdown displayed (focus)');
    });

    // Also show dropdown on click
    genreSearch.addEventListener('click', function() {
        genreDropdown.style.display = 'block';
        document.getElementById('genre-toggle-btn').classList.add('active');
        console.log('Genre dropdown displayed (click)');
    });

    // Toggle dropdown on button click
    document.getElementById('genre-toggle-btn').addEventListener('click', function(e) {
        e.preventDefault(); // Prevent form submission
        e.stopPropagation(); // Prevent event bubbling

        const isVisible = genreDropdown.style.display === 'block';

        if (isVisible) {
            genreDropdown.style.display = 'none';
            this.classList.remove('active');
            console.log('Genre dropdown hidden (toggle button)');
        } else {
            genreDropdown.style.display = 'block';
            this.classList.add('active');
            console.log('Genre dropdown displayed (toggle button)');
        }
    });

    // Handle clicks outside the genre dropdown
    document.addEventListener('click', function(e) {
        console.log('Document click event target:', e.target);
        console.log('Is inside genre-search-container:', !!e.target.closest('#genre-search-container'));
        console.log('Is inside selected-genres:', !!e.target.closest('.selected-genres'));
        console.log('Is genre-item:', !!e.target.closest('.genre-item'));
        console.log('Is genre-checkbox:', e.target.classList.contains('genre-checkbox'));

        // Don't hide dropdown if click is inside genre-search-container, selected-genres, or on a genre-item or checkbox
        if (!e.target.closest('#genre-search-container') && 
            !e.target.closest('.selected-genres') && 
            !e.target.closest('.genre-item') && 
            !e.target.classList.contains('genre-checkbox')) {

            genreDropdown.style.display = 'none';
            document.getElementById('genre-toggle-btn').classList.remove('active');
            console.log('Genre dropdown hidden');
        } else {
            console.log('Click inside dropdown area, keeping dropdown visible');
        }
    });

    // Handle keyboard navigation for genre dropdown
    genreSearch.addEventListener('keydown', function(e) {
        const genreItems = document.querySelectorAll('.genre-item');

        if (genreItems.length === 0 || genreDropdown.style.display === 'none') {
            return;
        }

        // Down arrow
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            selectedGenreIndex = (selectedGenreIndex + 1) % genreItems.length;
            updateSelectedGenreItem();
        }
        // Up arrow
        else if (e.key === 'ArrowUp') {
            e.preventDefault();
            selectedGenreIndex = selectedGenreIndex <= 0 ? genreItems.length - 1 : selectedGenreIndex - 1;
            updateSelectedGenreItem();
        }
        // Enter key
        else if (e.key === 'Enter' && selectedGenreIndex >= 0) {
            e.preventDefault();
            const items = document.querySelectorAll('.genre-item');
            if (items[selectedGenreIndex]) {
                items[selectedGenreIndex].click();
            }
        }
        // Escape key
        else if (e.key === 'Escape') {
            genreDropdown.style.display = 'none';
        }
    });

    // Function to update the selected genre item
    function updateSelectedGenreItem() {
        const items = document.querySelectorAll('.genre-item');

        // Remove selected class from all items
        items.forEach(item => {
            item.classList.remove('selected-nav');
        });

        // Add selected class to the current item
        if (selectedGenreIndex >= 0 && selectedGenreIndex < items.length) {
            items[selectedGenreIndex].classList.add('selected-nav');

            // Ensure the selected item is visible in the dropdown
            items[selectedGenreIndex].scrollIntoView({ block: 'nearest' });
        }
    }

    // Handle search form submission
    searchForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const extractSearchInput = document.getElementById('extract-search-input');

        currentTitle = searchInput.value;
        currentExtract = extractSearchInput.value;
        currentYear = yearFilter.value ? parseInt(yearFilter.value) : null;
        currentCast = parseCommaSeparatedValues(castFilter.value);
        // currentGenres is already updated when selecting genres

        searchMovies(currentTitle, currentExtract, currentCast, currentYear, currentGenres)
            .then(results => {
                displaySearchResults(results);
            });
    });

    // Function to update the selected autocomplete item
    function updateSelectedAutocompleteItem() {
        const items = document.querySelectorAll('.autocomplete-item');

        // Remove selected class from all items
        items.forEach(item => {
            item.classList.remove('selected');
        });

        // Add selected class to the current item
        if (selectedAutocompleteIndex >= 0 && selectedAutocompleteIndex < items.length) {
            items[selectedAutocompleteIndex].classList.add('selected');

            // Ensure the selected item is visible in the dropdown
            items[selectedAutocompleteIndex].scrollIntoView({ block: 'nearest' });
        }
    }
});
