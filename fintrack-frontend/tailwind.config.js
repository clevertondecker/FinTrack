/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#0052CC',
        accent: '#29C7AC',
        background: '#F5F7FA',
        text: '#2D2D2D',
        textSecondary: '#6B6B6B',
        darkBg: '#1F1F1F',
        lightText: '#E1E1E1',
      },
      fontFamily: {
        sans: ['Inter', 'Roboto', 'Poppins', 'sans-serif'],
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
}
