/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  safelist: [
    'bg-debate-left',
    'bg-debate-right',
    'text-debate-left',
    'text-debate-right',
    'border-debate-left',
    'border-debate-right',
    'border-r-debate-left',
    'border-l-debate-right',
    'border-t-transparent',
    'border-b-transparent',
    'border-l-0',
    'border-r-0',
    'border-r-[10px]',
    'border-l-[10px]',
    'border-t-[10px]',
    'border-b-[10px]',
    'left-[-9px]',
    'right-[-9px]',
    'left-[-12px]',
    'right-[-12px]',
    'rounded-sm',
    'tail-rounded-left',
    'tail-rounded-right',
    'top-2',
  ],
  theme: {
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
        // 토론방 커스텀 색상
        'debate-left': "#FACC15",   // 좌측 진영 배경 (찬성) - 밝은 황색
        'debate-right': "#B22222",  // 우측 진영 배경 (반대) - 밝은 적색
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
    },
  },
  plugins: [],
} 