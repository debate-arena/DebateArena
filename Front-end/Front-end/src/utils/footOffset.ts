export type FootOffsets = {
  naturalTransparentBottomPx: number;
  cssOffsetPx: number;
};

const cache = new Map<string, number>();

/** PNG 하단 투명여백(원본 px)을 계산 */
export async function measureTransparentBottomPx(src: string): Promise<number> {
  if (cache.has(src)) return cache.get(src)!;

  const img = await new Promise<HTMLImageElement>((resolve, reject) => {
    const im = new Image();
    im.crossOrigin = 'anonymous';
    im.onload = () => resolve(im);
    im.onerror = reject;
    im.src = src;
  });

  const { naturalWidth: W, naturalHeight: H } = img;
  const canvas = document.createElement('canvas');
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext('2d', { willReadFrequently: true } as any);
  if (!ctx) return 0;

  ctx.drawImage(img, 0, 0);
  const data = ctx.getImageData(0, 0, W, H).data;

  // 아래에서 위로 스캔: 알파>8 정도면 불투명으로 간주(안티앨리어싱 여유)
  const ALPHA_TH = 8;
  let y = H - 1;
  scan: for (; y >= 0; y--) {
    for (let x = 0; x < W; x++) {
      const idx = (y * W + x) * 4 + 3;
      if (data[idx] > ALPHA_TH) break scan;
    }
  }
  const transparentTail = Math.max(0, H - 1 - y);
  cache.set(src, transparentTail);
  return transparentTail;
}

/** 프레임 정사각형 S×S, object-contain 기준으로 CSS 보정px 계산 */
export function computeCssOffsetPx(
  frameSize: number,
  naturalWidth: number,
  naturalHeight: number,
  naturalTransparentBottomPx: number
): number {
  if (!frameSize || !naturalWidth || !naturalHeight) return 0;
  const scale = Math.min(frameSize / naturalWidth, frameSize / naturalHeight);
  return Math.round(naturalTransparentBottomPx * scale);
}


