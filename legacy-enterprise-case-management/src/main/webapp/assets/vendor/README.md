The application serves these pinned, unmodified distributions locally:

- Bootstrap 5.3.8 CSS and bundled JavaScript (including Popper):
  https://getbootstrap.com/docs/5.3/getting-started/introduction/
  Downloaded from https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/
- jQuery 3.7.1, full production build including AJAX:
  https://code.jquery.com/jquery-3.7.1.min.js

The distributions retain their license notices. Bootstrap and jQuery use the MIT
license. Bootstrap's bundled Popper also uses MIT. Source maps are optional
development artifacts and are not included. No third-party runtime requests are
needed for these assets. Load site.css after Bootstrap and site.js after both
JavaScript libraries in the shared Tiles layout.
