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
        // 토론방 커스텀 색상 (북극/남극 테마)
        'debate-left': "#F0F9FF",   // 북극곰 (쿨 화이트) - 아주 연한 얼음 하늘색
        'debate-right': "#0B1224",  // 펭귄 (네이비 블랙) - 남극 밤하늘 네이비
        'debate-random': "#C8D5E8", // 물범 (블루-그레이) - 푸른빛 도는 회색
        // 모드 선택 색상
        'mode-1v1': "#E0E7FF",      // 1vs1 - Indigo-100 (차가운 톤)
        'mode-2v2': "#CFFAFE",      // 2vs2 - 청록색 (Cyan-100)
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