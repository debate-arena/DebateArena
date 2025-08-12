<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed, nextTick } from 'vue'
import { measureTransparentBottomPx, computeCssOffsetPx } from '@/utils/footOffset'

const props = defineProps<{
  src: string
  selected?: boolean
  badgeText?: string
}>()
const emit = defineEmits<{ (e: 'select'): void }>()

const frameRef = ref<HTMLElement | null>(null)
const cssOffsetPx = ref(0)
const natural = ref({ w: 0, h: 0, tail: 0 })
let ro: ResizeObserver | null = null

async function calc() {
  const el = frameRef.value
  if (!el) return
  const S = Math.round(el.getBoundingClientRect().width)
  // 원본 치수 얻기
  const img = new Image()
  img.src = props.src
  try {
    await img.decode()
  } catch {}
  const w = img.naturalWidth || 0
  const h = img.naturalHeight || 0
  const tail = await measureTransparentBottomPx(props.src).catch(() => 0)
  natural.value = { w, h, tail }
  cssOffsetPx.value = computeCssOffsetPx(S, w, h, tail)
}

onMounted(async () => {
  await nextTick()
  await calc()
  ro = new ResizeObserver(() => calc())
  if (frameRef.value) ro.observe(frameRef.value)
})

onBeforeUnmount(() => {
  if (ro && frameRef.value) ro.unobserve(frameRef.value)
})

watch(() => props.src, () => calc())
const scaleClass = computed(() => (props.selected ? 'scale-125' : 'scale-100'))
</script>

<template>
  <div class="icon-frame relative aspect-square overflow-visible isolate" ref="frameRef">
    <!-- 스케일은 하단 기준으로 -->
    <div
      class="absolute inset-x-0 bottom-0 origin-bottom transition-transform duration-300 ease-out will-change-transform cursor-pointer"
      :class="scaleClass"
      role="button"
      aria-label="select icon"
      @click="$emit('select')"
    >
      <img
        :src="src"
        alt=""
        class="block w-full h-full object-contain object-bottom pointer-events-none select-none"
        :style="{ position: 'relative', bottom: `-${cssOffsetPx}px` }"
        draggable="false"
      />
    </div>

    <!-- 배지: 프레임 바닥선 공유(top:100%) -->
    <transition name="badge-pop" appear>
      <div
        v-if="selected"
        class="absolute left-1/2 top-full -translate-x-1/2 translate-y-4 h-10 md:h-11 px-6 rounded-2xl shadow-xl choice-plate pointer-events-none z-40 inline-flex items-center justify-center whitespace-nowrap text-center leading-none"
      >
        {{ badgeText }}
      </div>
    </transition>
  </div>
</template>

<style scoped>
@media (prefers-reduced-motion: reduce) {
  .transition-transform { transition: none !important; }
}
</style>

