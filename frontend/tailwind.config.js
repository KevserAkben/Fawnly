/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        'toz-pembe': 'var(--toz-pembe)',
        mint: 'var(--mint)',
        'pembe-koyu': 'var(--pembe-koyu)',
        'mint-koyu': 'var(--mint-koyu)',
      },
    },
  },
  plugins: [],
}
