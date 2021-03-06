module.exports = {
  plugins: ['vue', '@typescript-eslint'],
  parserOptions: {
    parser: '@typescript-eslint/parser',
    env: { es6: true },
    sourceType: 'module'
  },
  root: true,
  env: {
    browser: true,
    node: true,
    serviceworker: true
  },
  extends: ['plugin:vue/recommended', 'plugin:@typescript-eslint/recommended', '@vue/typescript/recommended'],
  rules: {
    'one-var': 0,
    'arrow-parens': 0,
    'generator-star-spacing': 0,
    'no-debugger': 1,
    'no-console': 1,
    semi: [2, 'always'],
    'no-extra-semi': 2,
    'space-before-function-paren': 1,
    eqeqeq: 0,
    'spaced-comment': 0,
    'no-useless-escape': 0,
    'no-tabs': 0,
    'no-mixed-spaces-and-tabs': 0,
    'keyword-spacing':1,
    'comma-spacing':1,
    'new-cap': 0,
    camelcase: 0,
    'no-new': 0,
    indent: [1, 2, { SwitchCase: 1 }],
    quotes: [1, 'single'],
    'comma-dangle': ['error', 'always-multiline'],
    '@typescript-eslint/explicit-function-return-type': 0,
    '@typescript-eslint/interface-name-prefix': 1,
    '@typescript-eslint/consistent-type-assertions': 1,
    '@typescript-eslint/no-explicit-any': 0,
    '@typescript-eslint/indent': [1, 2],
    '@typescript-eslint/quotes': [1, 'single'],
    '@typescript-eslint/semi': [2, 'always'],
    '@typescript-eslint/no-non-null-assertion': 'off',
    '@typescript-eslint/camelcase': 'off'
  }
};
