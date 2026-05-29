# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.2.2.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Internationalization

The application uses the local i18n service in `src/app/core/i18n/i18n.ts`.

Supported languages are `pt-BR`, `en`, and `es`. Portuguese (`pt-BR`) is the default language and fallback when a translation key is missing in another language.

Translation files are stored in `src/app/core/i18n/translations/`. Add new visible UI text as a key in `pt-br.ts` first, then provide the matching keys in `en.ts` and `es.ts`. Use module-oriented names such as `people.form.createError` or `layout.footer.projectGithub`.

Templates should use the standalone pipe:

```html
{{ 'people.title' | translate }}
```

TypeScript code should inject `I18nService` and call `translate()` for messages produced outside templates. The selected language is persisted in `localStorage` under `stella.language`.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
