import { defineConfig } from 'eslint/config'
import tseslint from '@electron-toolkit/eslint-config-ts'
import eslintConfigPrettier from '@electron-toolkit/eslint-config-prettier'
import eslintPluginVue from 'eslint-plugin-vue'
import vueParser from 'vue-eslint-parser'

export default defineConfig(
  { ignores: ['**/node_modules', '**/dist', '**/out'] },
  tseslint.configs.recommended,
  eslintPluginVue.configs['flat/recommended'],
  {
    rules: {
      'no-async-promise-executor': 'off',
      'no-case-declarations': 'off',
      'no-debugger': 'off',
      'no-useless-escape': 'off',
      'prettier/prettier': 'off',
      'vue/first-attribute-linebreak': 'off',
      'vue/no-mutating-props': 'off',
      'vue/require-valid-default-prop': 'off',
      'vue/v-on-event-hyphenation': 'off'
    }
  },
  {
    files: ['**/*.vue'],
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        ecmaFeatures: {
          jsx: true
        },
        extraFileExtensions: ['.vue'],
        parser: tseslint.parser
      }
    }
  },
  {
    files: ['**/*.{ts,mts,tsx,vue}'],
    rules: {
      '@typescript-eslint/ban-ts-comment': 'off',
      '@typescript-eslint/explicit-function-return-type': 'off',
      '@typescript-eslint/no-empty-object-type': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-unused-vars': 'off',
      '@typescript-eslint/no-wrapper-object-types': 'off',
      'no-unsafe-finally': 'off',
      'prefer-const': 'off',
      'prettier/prettier': 'off',
      'vue/attribute-hyphenation': 'off',
      'vue/attributes-order': 'off',
      'vue/block-lang': 'off',
      'vue/no-required-prop-with-default': 'off',
      'vue/no-reserved-component-names': 'off',
      'vue/no-side-effects-in-computed-properties': 'off',
      'vue/no-template-shadow': 'off',
      'vue/no-unused-vars': 'off',
      'vue/require-v-for-key': 'off',
      'vue/valid-v-for': 'off',
      'vue/require-default-prop': 'off',
      'vue/multi-word-component-names': 'off'
    }
  },
  eslintConfigPrettier
)
