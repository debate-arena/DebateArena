import { ref, onUnmounted, reactive, getCurrentInstance } from 'vue'

/**
 * 음성 제어 관련 Composable
 * - 음소거 기능
 * - 음성 변조 기능
 * - 참가자별 볼륨 제어
 */
let audioControlsSingleton: any | null = null

export function useAudioControls() {
  // 싱글턴: 이미 생성된 인스턴스가 있으면 재사용
  if (audioControlsSingleton) return audioControlsSingleton
  const pendingAudioStreams = new Map<string, MediaStream>()

  // 음성 관련 상태
  const isMuted = ref(false)
  const isVoiceModulated = ref(false)
  const localAudioTrack = ref<MediaStreamTrack | null>(null)
  
  // 참가자별 음성 제어
  const participantAudios = new Map<string, HTMLAudioElement>()
  const participantVolumes = reactive(new Map<string, number>())
  const participantMuted = reactive(new Map<string, boolean>())
  const participantMuteState = reactive<Record<string, boolean>>({})
  const participantSpeakingState = reactive<Record<string, boolean>>({})

  // 오디오 레벨 분석용 상태
  const analyserNodes = new Map<string, AnalyserNode>()
  const sourceNodes = new Map<string, MediaStreamAudioSourceNode>()
  const speakingEnergies = new Map<string, number>()
  const LOCAL_SPEAKING_KEY = '__local__'
  let speakingRafId: number | null = null

  // 내부 유틸: RMS 계산
  const computeRmsFromAnalyser = (analyser: AnalyserNode): number => {
    const bufferLength = analyser.fftSize
    const dataArray = new Uint8Array(bufferLength)
    analyser.getByteTimeDomainData(dataArray)
    let sumSquares = 0
    for (let i = 0; i < bufferLength; i++) {
      const v = (dataArray[i] - 128) / 128
      sumSquares += v * v
    }
    return Math.sqrt(sumSquares / bufferLength)
  }

  // 발화 모니터 루프 시작
  const startSpeakingMonitor = () => {
    const step = () => {
      analyserNodes.forEach((analyser, key) => {
        try {
          const rms = computeRmsFromAnalyser(analyser)
          const prev = speakingEnergies.get(key) ?? 0
          const energy = prev * 0.85 + rms * 0.15 // 지수 평활
          speakingEnergies.set(key, energy)
          const THRESHOLD = 0.035
          participantSpeakingState[key] = energy > THRESHOLD
        } catch {}
      })
      speakingRafId = requestAnimationFrame(step)
    }
    speakingRafId = requestAnimationFrame(step)
  }
  
  // 음성 변조 관련 상태
  const audioContext = ref<AudioContext | null>(null)
  const sourceNode = ref<MediaStreamAudioSourceNode | null>(null)
  const modulatedStream = ref<MediaStream | null>(null)
  const originalTrack = ref<MediaStreamTrack | null>(null)

  // AudioContext 초기화
  const initAudioContext = () => {
    if (!audioContext.value) {
      audioContext.value = new (window.AudioContext || (window as any).webkitAudioContext)()
    }
    return audioContext.value
  }

  // 참가자 오디오 엘리먼트 설정
  const setParticipantAudio = (userEmail: string, audioElement: HTMLAudioElement | null) => {
    if (!audioElement) return;
    
    participantAudios.set(userEmail, audioElement)
      
      // 오디오 엘리먼트 기본 설정
      audioElement.autoplay = true
      audioElement.setAttribute('playsinline', 'true')
      audioElement.muted = participantMuted.get(userEmail) || false
      
      // 기본 볼륨 80%로 설정 (더 잘 들리도록)
      if (!participantVolumes.has(userEmail)) {
        participantVolumes.set(userEmail, 0.8)
        audioElement.volume = 0.8
      } else {
        audioElement.volume = participantVolumes.get(userEmail) ?? 0.8
      }

      const queuedStream = pendingAudioStreams.get(userEmail)
      if (queuedStream) {
        connectParticipantAudio(userEmail, queuedStream)
        pendingAudioStreams.delete(userEmail)
      }
      
      console.log(`🎵 참가자 ${userEmail}의 오디오 엘리먼트 설정됨 (볼륨: ${Math.round(audioElement.volume * 100)}%)`)
    }

  // 참가자 볼륨 가져오기
  const getParticipantVolume = (userEmail: string): number => {
    return participantVolumes.get(userEmail) ?? 0.8
  }

  // 참가자 볼륨 설정
  const setParticipantVolume = (userEmail: string, volume: string | number) => {
    let volumeValue = typeof volume === 'string' ? parseFloat(volume) : volume
    if (Number.isNaN(volumeValue as number)) volumeValue = 0
    // 0~1 사이로 클램프
    volumeValue = Math.min(1, Math.max(0, volumeValue as number))
    participantVolumes.set(userEmail, volumeValue)
    
    const audioElement = participantAudios.get(userEmail)
    if (audioElement) {
      audioElement.volume = volumeValue as number
      console.log(`🔊 참가자 ${userEmail}의 볼륨을 ${Math.round(volumeValue * 100)}%로 설정`)
    }
  }

  // 참가자 음소거 상태 가져오기
  const getParticipantMuted = (userEmail: string): boolean => {
    return participantMuteState[userEmail] ?? false
  }

  // 참가자 음소거 설정
  const setParticipantMuted = (userEmail: string, muted: boolean) => {
    participantMuted.set(userEmail, muted)
    participantMuteState[userEmail] = muted
    const audioElement = participantAudios.get(userEmail)
    if (audioElement) {
      audioElement.muted = muted
    }
    console.log(`🔇 참가자 ${userEmail} ${muted ? '음소거' : '음소거 해제'}`)
  }

  // 참가자 음소거 토글
  const toggleParticipantMuted = (userEmail: string) => {
    const next = !getParticipantMuted(userEmail)
    console.log(`🔇 참가자 ${userEmail} ${next ? '음소거' : '음소거 해제'}`)
    setParticipantMuted(userEmail, next)
  }

  // 참가자 오디오 스트림 연결
  const connectParticipantAudio = (userEmail: string, audioStream: MediaStream) => {
    const audioElement = participantAudios.get(userEmail)
    if (!audioElement) {
      pendingAudioStreams.set(userEmail, audioStream)
      return
    }
    audioElement.srcObject = audioStream
    if (audioElement && audioStream) {
      console.log(`🔊 참가자 ${userEmail}의 스트림 연결 시작...`)
      
      // 스트림 연결
      audioElement.srcObject = audioStream
      
      // 저장된 볼륨 설정 적용
      const volume = participantVolumes.get(userEmail) ?? 0.8
      audioElement.volume = volume
      
      // 오디오 트랙 확인
      const audioTracks = audioStream.getAudioTracks()
      console.log(`🎵 오디오 트랙 수: ${audioTracks.length}`)
      
      if (audioTracks.length > 0) {
        const track = audioTracks[0]
        console.log(`🎵 오디오 트랙 상태: enabled=${track.enabled}, readyState=${track.readyState}`)
        
        // 트랙이 비활성화되어 있다면 활성화
        if (!track.enabled) {
          track.enabled = true
        }
      }
      
      // 다양한 이벤트 리스너 추가 (디버깅용)
      audioElement.addEventListener('loadstart', () => {
        console.log(`📡 ${userEmail}: loadstart`)
      }, { once: true })
      
      audioElement.addEventListener('loadeddata', () => {
        console.log(`📡 ${userEmail}: loadeddata`)
      }, { once: true })
      
      audioElement.addEventListener('canplay', () => {
        console.log(`📡 ${userEmail}: canplay`)
      }, { once: true })
      
      audioElement.addEventListener('playing', () => {
        console.log(`🎵 ${userEmail}: 재생 시작됨!`)
      }, { once: true })
      
      audioElement.addEventListener('error', (e) => {
        console.error(`❌ ${userEmail}: 오디오 오류`, e)
      }, { once: true })
      
      // 강제 재생 시도
      setTimeout(() => {
        if (audioElement.paused) {
          audioElement.play().then(() => {
            console.log(`✅ ${userEmail}: 자동 재생 성공`)
          }).catch(error => {
            console.warn(`⚠️ ${userEmail}: 자동 재생 실패`, error)
            
            // 사용자 상호작용 후 재시도
            const playOnInteraction = () => {
              audioElement.play().then(() => {
                console.log(`✅ ${userEmail}: 수동 재생 성공`)
              }).catch(console.error)
              document.removeEventListener('click', playOnInteraction)
              document.removeEventListener('touchstart', playOnInteraction)
            }
            document.addEventListener('click', playOnInteraction, { once: true })
            document.addEventListener('touchstart', playOnInteraction, { once: true })
          })
        }
      }, 100)
      
      console.log(`🔊 참가자 ${userEmail}의 스트림 연결 완료 (볼륨: ${Math.round(volume * 100)}%)`)
      // 오디오 분석(발화 감지) 연결
      try {
        const ctx = initAudioContext()
        // 일부 브라우저에서 초기 상태가 suspended일 수 있음
        ctx.resume?.().catch(() => {})
        // 기존 소스/분석기 정리
        try { sourceNodes.get(userEmail)?.disconnect() } catch {}
        try { analyserNodes.get(userEmail)?.disconnect() } catch {}
        const source = ctx.createMediaStreamSource(audioStream)
        const analyser = ctx.createAnalyser()
        analyser.fftSize = 512
        analyser.smoothingTimeConstant = 0.2
        source.connect(analyser)
        sourceNodes.set(userEmail, source)
        analyserNodes.set(userEmail, analyser)
        if (!speakingRafId) startSpeakingMonitor()
      } catch (e) {
        console.warn('발화 감지 초기화 실패:', e)
      }
    } else {
      if (!audioElement) {
        console.warn(`⚠️ 참가자 ${userEmail}의 audio 엘리먼트가 없음`)
      }
      if (!audioStream) {
        console.warn(`⚠️ 참가자 ${userEmail}의 오디오 스트림이 없음`)
      }
    }
  }

  // 참가자 오디오 스트림 분리
  const disconnectParticipantAudio = (userEmail: string) => {
    const audioElement = participantAudios.get(userEmail)
    if (audioElement) {
      const currentStream = audioElement.srcObject as MediaStream | null
      if (currentStream) {
        currentStream.getTracks().forEach(track => {
          try { track.stop() } catch {}
        })
      }
      audioElement.srcObject = null
    }
    pendingAudioStreams.delete(userEmail)
    participantAudios.delete(userEmail)
    participantVolumes.delete(userEmail)
    participantMuted.delete(userEmail)
    delete participantMuteState[userEmail]
    console.log(`🔇 참가자 ${userEmail}의 오디오 연결 해제`)
  }

  // 로컬 오디오 트랙 설정
  const setLocalAudioTrack = (track: MediaStreamTrack | null) => {
    localAudioTrack.value = track
    console.log('🎤 로컬 오디오 트랙 설정됨')
    // 로컬 발화 감지 연결
    try {
      // 기존 로컬 소스/분석기 정리
      try { sourceNodes.get(LOCAL_SPEAKING_KEY)?.disconnect() } catch {}
      try { analyserNodes.get(LOCAL_SPEAKING_KEY)?.disconnect() } catch {}
      if (track) {
        const ctx = initAudioContext()
        ctx.resume?.().catch(() => {})
        const localStream = new MediaStream([track])
        const source = ctx.createMediaStreamSource(localStream)
        const analyser = ctx.createAnalyser()
        analyser.fftSize = 512
        analyser.smoothingTimeConstant = 0.2
        source.connect(analyser)
        sourceNodes.set(LOCAL_SPEAKING_KEY, source)
        analyserNodes.set(LOCAL_SPEAKING_KEY, analyser)
        if (!speakingRafId) startSpeakingMonitor()
      } else {
        sourceNodes.delete(LOCAL_SPEAKING_KEY)
        analyserNodes.delete(LOCAL_SPEAKING_KEY)
      }
    } catch (e) {
      console.warn('로컬 발화 감지 연결 실패:', e)
    }
  }

  // 음소거 토글
  const toggleMute = () => {
    if (localAudioTrack.value) {
      isMuted.value = !isMuted.value
      localAudioTrack.value.enabled = !isMuted.value
      console.log(`🎤 마이크 ${isMuted.value ? '음소거됨' : '활성화됨'}`)
    } else {
      console.warn('로컬 오디오 트랙이 없습니다.')
    }
  }

  // AudioWorklet 프로세서 정의 (인라인) - 고유한 이름 사용
  const createPitchShiftProcessor = () => {
    // 고유한 프로세서 이름 생성
    const processorName = `pitch-shift-processor-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    
    const processorCode = `
      class PitchShiftProcessor extends AudioWorkletProcessor {
        constructor() {
          super();
          this.pitchRatio = 5; // 5배 높은 피치
          this.bufferIndex = 0;
          this.buffer = new Float32Array(8192);
        }

        process(inputs, outputs, parameters) {
          const input = inputs[0];
          const output = outputs[0];
          
          if (input.length > 0 && output.length > 0) {
            const inputChannel = input[0];
            const outputChannel = output[0];
            
            // 간단한 피치 시프팅 알고리즘
            for (let i = 0; i < outputChannel.length; i++) {
              const sampleIndex = Math.floor(i / this.pitchRatio);
              if (sampleIndex < inputChannel.length) {
                outputChannel[i] = inputChannel[sampleIndex] * 0.8; // 볼륨 조정
              } else {
                outputChannel[i] = 0;
              }
            }
          }
          
          return true;
        }
      }

      registerProcessor('${processorName}', PitchShiftProcessor);
    `
    
    const blob = new Blob([processorCode], { type: 'application/javascript' })
    return { 
      url: URL.createObjectURL(blob), 
      name: processorName 
    }
  }

  // 음성 변조 적용 (피치 높이기) - AudioWorkletNode 사용
  const applyVoiceModulation = async () => {
    try {
      const ctx = initAudioContext()
      
      if (!localAudioTrack.value) {
        throw new Error('로컬 오디오 트랙이 없습니다.')
      }

      // 원본 트랙 저장
      originalTrack.value = localAudioTrack.value
      
      // 새로운 MediaStream 생성 (변조용)
      const originalStream = new MediaStream([localAudioTrack.value])
      sourceNode.value = ctx.createMediaStreamSource(originalStream)
      
      try {
        // AudioWorklet 프로세서 등록
        const processorInfo = createPitchShiftProcessor()
        await ctx.audioWorklet.addModule(processorInfo.url)
        
        // AudioWorkletNode 생성 (고유한 프로세서 이름 사용)
        const pitchShiftNode = new AudioWorkletNode(ctx, processorInfo.name)
        
        // Gain 노드 생성
        const gainNode = ctx.createGain()
        gainNode.gain.value = 1.2 // 약간 볼륨 증가
        
        // MediaStreamDestination 생성
        const destination = ctx.createMediaStreamDestination()
        
        // 오디오 노드 연결
        sourceNode.value
          .connect(pitchShiftNode)
          .connect(gainNode)
          .connect(destination)
        
        // 변조된 스트림 저장
        modulatedStream.value = destination.stream
        const modulatedTrack = modulatedStream.value.getAudioTracks()[0]
        
        if (modulatedTrack) {
          // Producer에서 사용하는 트랙을 변조된 트랙으로 교체
          localAudioTrack.value = modulatedTrack
          console.log('✅ 음성 변조 적용 완료 (피치 상승 5배 - AudioWorklet)')
        }
        
        // 프로세서 URL 정리
        URL.revokeObjectURL(processorInfo.url)
        
      } catch (workletError) {
        console.warn('AudioWorklet 사용 실패, 대체 방법 사용:', workletError)
        
        // AudioWorklet을 사용할 수 없는 경우 간단한 필터 체인 사용
        const gainNode1 = ctx.createGain()
        gainNode1.gain.value = 1.5
        
        // 고주파 강조 필터
        const highpassFilter = ctx.createBiquadFilter()
        highpassFilter.type = 'highpass'
        highpassFilter.frequency.value = 1000
        highpassFilter.Q.value = 1
        
        // 추가 gain
        const gainNode2 = ctx.createGain()
        gainNode2.gain.value = 1.2
        
        // MediaStreamDestination 생성
        const destination = ctx.createMediaStreamDestination()
        
        // 오디오 노드 연결
        sourceNode.value
          .connect(gainNode1)
          .connect(highpassFilter)
          .connect(gainNode2)
          .connect(destination)
        
        // 변조된 스트림 저장
        modulatedStream.value = destination.stream
        const modulatedTrack = modulatedStream.value.getAudioTracks()[0]
        
        if (modulatedTrack) {
          localAudioTrack.value = modulatedTrack
          console.log('✅ 음성 변조 적용 완료 (필터 체인 방식)')
        }
      }
      
    } catch (error) {
      console.error('❌ 음성 변조 적용 실패:', error)
    }
  }

  // 음성 변조 제거
  const removeVoiceModulation = async () => {
    try {
      // 원본 트랙으로 복원
      if (originalTrack.value) {
        localAudioTrack.value = originalTrack.value
        console.log('✅ 음성 변조 제거 완료 (원본 복원)')
      }
      
      // 음성 변조 관련 리소스 정리
      if (sourceNode.value) {
        sourceNode.value.disconnect()
        sourceNode.value = null
      }
      
      if (modulatedStream.value) {
        modulatedStream.value.getTracks().forEach(track => track.stop())
        modulatedStream.value = null
      }
      
    } catch (error) {
      console.error('❌ 음성 변조 제거 실패:', error)
    }
  }

  // 음성 변조 토글
  const toggleVoiceModulation = async () => {
    if (!localAudioTrack.value) {
      console.warn('로컬 오디오 트랙이 없습니다.')
      return
    }

    if (isVoiceModulated.value) {
      // 음성 변조 비활성화
      await removeVoiceModulation()
    } else {
      // 음성 변조 활성화
      await applyVoiceModulation()
    }
    
    isVoiceModulated.value = !isVoiceModulated.value
    console.log(`🎭 음성 변조 ${isVoiceModulated.value ? '활성화됨' : '비활성화됨'}`)
  }

  // 모든 참가자 오디오 정리
  const clearAllParticipantAudios = () => {
    participantAudios.clear()
    participantVolumes.clear()
    participantMuted.clear()
    for (const key in participantMuteState) delete participantMuteState[key]
    console.log('🧹 모든 참가자 오디오 정리됨')
  }

  // 리소스 정리
  const cleanup = () => {
    // 음성 변조 관련 리소스 정리
    if (isVoiceModulated.value) {
      removeVoiceModulation()
    }
    
    if (audioContext.value) {
      audioContext.value.close()
      audioContext.value = null
    }
    
    if (sourceNode.value) {
      sourceNode.value.disconnect()
      sourceNode.value = null
    }
    
    if (modulatedStream.value) {
      modulatedStream.value.getTracks().forEach(track => track.stop())
      modulatedStream.value = null
    }
    
    // 로컬 오디오 트랙 정리
    if (localAudioTrack.value) {
      localAudioTrack.value.stop()
      localAudioTrack.value = null
    }
    
    if (originalTrack.value) {
      originalTrack.value.stop()
      originalTrack.value = null
    }
    
    // 참가자 audio 엘리먼트 및 볼륨 정리
    clearAllParticipantAudios()
    // 발화 감지 정리
    if (speakingRafId) {
      cancelAnimationFrame(speakingRafId)
      speakingRafId = null
    }
    analyserNodes.forEach(node => { try { node.disconnect() } catch {} })
    sourceNodes.forEach(node => { try { node.disconnect() } catch {} })
    analyserNodes.clear()
    sourceNodes.clear()
    speakingEnergies.clear()
    for (const key in participantSpeakingState) delete participantSpeakingState[key]
    
    console.log('🧹 음성 리소스 정리 완료')
  }

  // 컴포넌트 언마운트 시 자동 정리 (컴포넌트 컨텍스트가 있을 때만)
  if (getCurrentInstance()) {
    onUnmounted(() => {
      cleanup()
    })
  }

  const api = {
    // 상태
    isMuted,
    isVoiceModulated,
    localAudioTrack,
    
    // 음소거 기능
    toggleMute,
    
    // 음성 변조 기능
    toggleVoiceModulation,
    applyVoiceModulation,
    removeVoiceModulation,
    
    // 참가자 음성 제어
    setParticipantAudio,
    getParticipantVolume,
    setParticipantVolume,
    getParticipantMuted,
    setParticipantMuted,
    toggleParticipantMuted,
    connectParticipantAudio,
    disconnectParticipantAudio,
    // 발화 감지 API
    getParticipantSpeaking: (userEmail: string): boolean => {
      return participantSpeakingState[userEmail] ?? false
    },
    getLocalSpeaking: (): boolean => {
      return participantSpeakingState[LOCAL_SPEAKING_KEY] ?? false
    },
    
    // 로컬 오디오 관리
    setLocalAudioTrack,
    
    // 정리 함수
    clearAllParticipantAudios,
    cleanup
  }

  // 싱글턴 저장 후 반환
  audioControlsSingleton = api
  return api
}