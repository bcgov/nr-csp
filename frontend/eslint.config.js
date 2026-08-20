import js from '@eslint/js';
import tanstackQuery from '@tanstack/eslint-plugin-query';
import prettier from 'eslint-config-prettier';
import importPlugin from 'eslint-plugin-import';
import prettierPlugin from 'eslint-plugin-prettier';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['dist/', 'build/', 'node_modules/'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  // eslint-plugin-react-hooks v7 moved flat configs under `configs.flat`
  reactHooks.configs.flat['recommended-latest'],
  reactRefresh.configs.vite,
  prettier,
  {
    languageOptions: {
      ecmaVersion: 2022,
      globals: { window: 'readonly', document: 'readonly', navigator: 'readonly' },
    },
    plugins: {
      '@tanstack/query': tanstackQuery,
      import: importPlugin,
      prettier: prettierPlugin,
    },
    settings: {
      'import/resolver': {
        typescript: {
          project: './tsconfig.app.json',
          extensions: ['.ts', '.tsx', '.js', '.jsx', '.json'],
        },
      },
    },
    rules: {
      '@tanstack/query/exhaustive-deps': 'warn',
      'react-hooks/exhaustive-deps': 'warn',
      // New React Compiler-powered rules in eslint-plugin-react-hooks v7:
      // surfaced as warnings until the flagged patterns are refactored.
      'react-hooks/set-state-in-effect': 'warn',
      'react-hooks/refs': 'warn',
      'react-hooks/immutability': 'warn',
      'import/order': [
        'warn',
        {
          groups: ['builtin', 'external', 'internal', 'parent', 'sibling', 'index', 'object', 'type'],
          alphabetize: { order: 'asc', caseInsensitive: true },
          'newlines-between': 'always',
        },
      ],
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          caughtErrorsIgnorePattern: '^_',
        },
      ],
      'no-console': 'warn',
      'no-debugger': 'warn',
      'prettier/prettier': 'warn',
    },
  },
  {
    // Node scripts (build/codegen) run outside the browser and use Node globals.
    files: ['**/*.mjs', 'scripts/**/*.{js,mjs}'],
    languageOptions: {
      globals: { process: 'readonly', console: 'readonly' },
    },
  },
  {
    // Test files lean on `any` for mocks and partial fixtures.
    files: ['**/*.test.{ts,tsx}', 'src/config/tests/**'],
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
    },
  },
);
