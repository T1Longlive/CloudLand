import pluginVue from 'eslint-plugin-vue'

/**
 * 前端静态检查（ESLint 9+/10 flat config）。
 *
 * 只卡"会真出 bug"的规则，不引入风格类规则（缩进、引号、分号一律不管）：
 * 这个仓库的历史代码量不小，风格规则一次性接入会产出一个几百行的 diff，
 * 而那个 diff 一次都 review 不完，最后一定是 `--fix` 一把梭 —— 那种"绿"没有意义。
 *
 * 想加规则的正确顺序：先看它在现有代码上有几条违规，能随手修的修掉，
 * 剩下的用 注释 disable + 理由 顶住，规则才加得进来。
 */
export default [
  { ignores: ['dist/**', 'node_modules/**', 'public/**', 'coverage/**'] },
  ...pluginVue.configs['flat/base'],
  {
    files: ['**/*.{js,vue}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        window: 'readonly',
        document: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        console: 'readonly',
        process: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
        require: 'readonly',
        location: 'readonly',
        URL: 'readonly',
        URLSearchParams: 'readonly',
        FormData: 'readonly',
        Blob: 'readonly',
        FileReader: 'readonly',
        fetch: 'readonly',
        alert: 'readonly',
        confirm: 'readonly',
      },
    },
    rules: {
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      'no-undef': 'error',
      'no-dupe-keys': 'error',
      'no-unreachable': 'error',
      'vue/no-parsing-error': 'error',
      'vue/no-duplicate-attributes': 'error',
      'vue/no-use-v-if-with-v-for': 'error',
      'vue/require-v-for-key': 'error',
      'vue/no-side-effects-in-computed-properties': 'error',
      'vue/no-mutating-props': 'error',
    },
  },
]
