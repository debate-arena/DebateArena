<template>
  <div
    class="bg-gradient-to-br relative"
    :class="[isPreparationTime ? 'min-h-[max(800px,calc(100vh-64px))]' : '']"
  >
    <!-- 시작 전 화면 -->
    <div v-if="isPreparationTime">
      <!-- WebRTC 연결 중 화면 -->
      <div
        v-if="state.isConnecting || !isTimerStarted"
        class="absolute inset-0 z-50 bg-black/90 flex items-center justify-center"
      >
        <div class="text-center">
          <!-- 로딩 애니메이션 -->
          <div class="mb-8">
            <div class="relative w-32 h-32 mx-auto">
              <!-- 외부 원 -->
              <div
                class="absolute inset-0 border-4 border-blue-200 rounded-full"
              ></div>
              <!-- 회전하는 원 -->
              <div
                class="absolute inset-0 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"
              ></div>
              <!-- 내부 원 -->
              <div
                class="absolute inset-4 border-2 border-green-300 rounded-full opacity-60"
              ></div>
              <div
                class="absolute inset-4 border-2 border-green-500 border-t-transparent rounded-full animate-spin"
                style="animation-direction: reverse; animation-duration: 1.5s"
              ></div>

              <!-- 중앙 아이콘 -->
              <div class="absolute inset-0 flex items-center justify-center">
                <div class="text-4xl">🔗</div>
              </div>
            </div>
          </div>

          <!-- 연결 상태 텍스트 -->
          <div class="text-3xl text-white mb-4 font-bold">참가자 연결 중</div>

          <!-- 상세 진행 상태 -->
          <div class="text-lg text-gray-300 mb-8">
            {{ state.connectionStepText }}
          </div>

          <!-- 진행 단계 표시 -->
          <div class="flex justify-center items-center space-x-4 mb-8">
            <div
              v-for="(step, index) in [
                'auth',
                'router',
                'recv',
                'send',
                'consumer',
                'completed',
              ]"
              :key="step"
              :class="[
                'w-4 h-4 rounded-full transition-all duration-500',
                connectionStep === step ? 'bg-blue-500' : 'bg-gray-600',
              ]"
            ></div>
          </div>

          <!-- 참가자 연결 상태 -->
          <div class="mt-12 w-full max-w-4xl mx-auto">
            <!-- 좌측 진영과 우측 진영을 구분하여 표시 -->
            <div class="flex flex-wrap justify-between items-center gap-4">
              <!-- 좌측 진영 -->
              <div class="flex-1 min-w-[300px] text-center">
                <div class="flex justify-center space-x-2">
                  <div
                    v-for="participant in roomStore.leftTeam"
                    :key="participant.userId"
                    class="flex flex-col items-center space-y-2 w-32"
                  >
                    <div class="relative">
                      <Avatar :class="['w-16 h-16']">
                        <AvatarImage
                          :src="debateLeftProfile"
                          alt="좌측 진영"
                          class="border-2 border-black rounded-full bg-white"
                        />
                      </Avatar>
                      <!-- 연결 상태 표시 -->
                      <div
                        :class="[
                          'absolute -bottom-1 -right-1 w-6 h-6 rounded-full border-2 border-white flex items-center justify-center',
                          state.participants.find(
                            (p) => p.producerUserEmail === participant.userId
                          )?.connected ||
                          (participant.userId === debateStore.myEmail &&
                            state.isConnected)
                            ? 'bg-green-500'
                            : 'bg-gray-400',
                        ]"
                      >
                        <div
                          :class="[
                            'w-3 h-3 rounded-full',
                            state.participants.find(
                              (p) => p.producerUserEmail === participant.userId
                            )?.connected ||
                            (participant.userId === debateStore.myEmail &&
                              state.isConnected)
                              ? 'bg-white'
                              : 'bg-white animate-pulse',
                          ]"
                        ></div>
                      </div>
                    </div>
                    <span
                      class="text-sm text-white text-center font-bold max-w-full break-words h-[2.6rem] flex justify-center items-center"
                      style="
                        word-break: keep-all;
                        overflow-wrap: break-word;
                        line-height: 1.2;
                        white-space: pre-line;
                      "
                    >
                      {{
                        participant.displayName.length > 8
                          ? participant.displayName.slice(0, 8) +
                            "\n" +
                            participant.displayName.slice(8)
                          : participant.displayName
                      }}
                    </span>
                    <span
                      class="text-xs px-2 py-1 rounded-full bg-gray-500/20 text-gray-300"
                    >
                      {{
                        state.participants.find(
                          (p) => p.producerUserEmail === participant.userId
                        )?.connected ||
                        (participant.userId === debateStore.myEmail &&
                          state.isConnected)
                          ? "연결됨"
                          : "연결 중..."
                      }}
                    </span>
                  </div>
                </div>
              </div>

              <!-- VS -->
              <div class="w-full sm:w-auto text-center my-4 sm:my-0">
                <div class="text-4xl font-bold text-red-500">VS</div>
              </div>

              <!-- 우측 진영 -->
              <div class="flex-1 min-w-[300px] text-center">
                <div class="flex justify-center space-x-2">
                  <div
                    v-for="participant in roomStore.rightTeam"
                    :key="participant.userId"
                    class="flex flex-col items-center space-y-2 w-32"
                  >
                    <div class="relative">
                      <Avatar :class="['w-16 h-16']">
                        <AvatarImage
                          :src="debateRightProfile"
                          alt="우측 진영"
                          class="border-2 border-black rounded-full bg-white"
                        />
                      </Avatar>
                      <!-- 연결 상태 표시 -->
                      <div
                        :class="[
                          'absolute -bottom-1 -right-1 w-6 h-6 rounded-full border-2 border-white flex items-center justify-center',
                          state.participants.find(
                            (p) => p.producerUserEmail === participant.userId
                          )?.connected ||
                          (participant.userId === debateStore.myEmail &&
                            state.isConnected)
                            ? 'bg-green-500'
                            : 'bg-gray-400',
                        ]"
                      >
                        <div
                          :class="[
                            'w-3 h-3 rounded-full',
                            state.participants.find(
                              (p) => p.producerUserEmail === participant.userId
                            )?.connected ||
                            (participant.userId === debateStore.myEmail &&
                              state.isConnected)
                              ? 'bg-white'
                              : 'bg-white animate-pulse',
                          ]"
                        ></div>
                      </div>
                    </div>
                    <span
                      class="text-sm text-white text-center max-w-full break-words font-bold h-[2.6rem] flex justify-center items-center"
                      style="
                        word-break: keep-all;
                        overflow-wrap: break-word;
                        line-height: 1.2;
                        white-space: pre-line;
                      "
                    >
                      {{
                        participant.displayName.length > 8
                          ? participant.displayName.slice(0, 8) +
                            "\n" +
                            participant.displayName.slice(8)
                          : participant.displayName
                      }}
                    </span>
                    <span
                      class="text-xs px-2 py-1 rounded-full bg-gray-500/20 text-gray-300"
                    >
                      {{
                        state.participants.find(
                          (p) => p.producerUserEmail === participant.userId
                        )?.connected ||
                        (participant.userId === debateStore.myEmail &&
                          state.isConnected)
                          ? "연결됨"
                          : "연결 중..."
                      }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 준비시간 화면 -->
      <div
        v-else-if="isTimerStarted"
        class="absolute inset-0 z-50 bg-black bg-opacity-80 flex items-center justify-center"
      >
        <div class="text-center">
          <!-- 메인 카운트다운 숫자 -->
          <div
            :class="[
              'text-9xl font-bold mb-8 animate-pulse',
              preparationTimeLeft <= 10 ? 'text-red-500' : 'text-white',
            ]"
          >
            {{ preparationTimeLeft }}
          </div>

          <!-- 준비시간 텍스트 -->
          <div class="text-3xl text-white mb-4 font-bold">준비시간</div>

          <!-- 설명 텍스트 -->
          <div class="text-lg text-gray-300 mb-8 font-bold">
            토론이 곧 시작됩니다. 마음의 준비를 하세요!
          </div>

          <!-- 프로그레스 바 -->
          <div class="w-96 bg-gray-700 rounded-full h-3 mx-auto mb-8">
            <div
              :class="[
                'h-3 rounded-full transition-all duration-1000 ease-linear',
                preparationTimeLeft <= 10 ? 'bg-red-500' : 'bg-blue-500',
              ]"
              :style="{
                width: `${((preparationTimeLeft || 0) / (PREPARATION_DURATION / 1000)) * 100}%`,
              }"
            ></div>
          </div>

          <!-- 토론 주제 -->

          <div class="text-4xl font-bold text-white mb-8">
            {{ debateSubject }}
          </div>

          <!-- 양 진영 정보 미리보기 -->
          <div class="mt-12 w-full max-w-4xl mx-auto">
            <div
              class="grid grid-cols-2 items-center gap-8"
              style="grid-template-columns: 1fr auto 1fr"
            >
              <!-- 좌측 진영 -->
              <div class="text-center">
                <div class="text-2xl font-bold text-white mb-4">
                  {{ debateLeftTeam }}
                </div>
                <div class="flex justify-center space-x-2">
                  <div
                    v-for="participant in roomStore.leftTeam"
                    :key="participant.userId"
                    class="flex flex-col items-center space-y-2 w-32"
                  >
                    <Avatar :class="['w-16 h-16']">
                      <AvatarImage
                        :src="debateLeftProfile"
                        :alt="participant.displayName"
                        class="border-2 border-black rounded-full bg-white"
                      />
                    </Avatar>
                    <span
                      class="text-sm text-white text-center max-w-full break-words font-bold h-[2.6rem] flex justify-center items-center"
                      style="
                        word-break: keep-all;
                        overflow-wrap: break-word;
                        line-height: 1.2;
                        white-space: pre-line;
                      "
                    >
                      {{
                        participant.displayName.length > 8
                          ? participant.displayName.slice(0, 8) +
                            "\n" +
                            participant.displayName.slice(8)
                          : participant.displayName
                      }}
                    </span>
                    <span class="px-2 py-1 text-white font-bold">
                      {{ speakingOrderMap[participant.userId] }}번
                    </span>
                  </div>
                </div>
              </div>

              <!-- VS (중앙, 작은 비중) -->
              <div class="flex justify-center items-center px-4">
                <div class="text-4xl font-bold text-red-500">VS</div>
              </div>

              <!-- 우측 진영 -->
              <div class="text-center">
                <div class="text-2xl font-bold text-white mb-4">
                  {{ debateRightTeam }}
                </div>
                <div class="flex justify-center space-x-2">
                  <div
                    v-for="participant in roomStore.rightTeam"
                    :key="participant.userId"
                    class="flex flex-col items-center space-y-2 w-32"
                  >
                    <Avatar :class="['w-16 h-16']">
                      <AvatarImage
                        :src="debateRightProfile"
                        :alt="participant.displayName"
                        class="border-2 border-white rounded-full bg-white"
                      />
                    </Avatar>
                    <span
                      class="text-sm text-white text-center max-w-full break-words font-bold h-[2.6rem] flex justify-center items-center"
                      style="
                        word-break: keep-all;
                        overflow-wrap: break-word;
                        line-height: 1.2;
                        white-space: pre-line;
                      "
                    >
                      {{
                        participant.displayName.length > 8
                          ? participant.displayName.slice(0, 8) +
                            "\n" +
                            participant.displayName.slice(8)
                          : participant.displayName
                      }}
                    </span>
                    <span class="px-2 py-1 text-white font-bold">
                      {{ speakingOrderMap[participant.userId] }}번
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 메인 화면 -->
    <div
      v-else
      class="container mx-auto max-w-7xl flex h-full"
      style="min-height: 600px; min-width: 600px"
    >
      <!-- 좌측: 토론 내용 영역 -->
      <div
        class="flex flex-col min-w-0 p-6 pr-3"
        style="flex: 2; min-width: 600px"
      >
        <!-- 상단 토론 정보 영역 (축소/확장 가능) -->
        <Card
          class="bg-white"
          :class="[
            'mb-6 flex-shrink-0 transition-all duration-500 ease-in-out overflow-hidden relative pb-0',
            isDebateInfoCollapsed ? 'h-12' : 'max-h-screen',
          ]"
        >
          <!-- 전체 정보 표시 -->
          <CardContent
            :class="[
              'transition-all duration-500 ease-in-out',
              isDebateInfoCollapsed
                ? 'opacity-0 translate-y-4 pointer-events-none absolute inset-0'
                : 'opacity-100 translate-y-0 relative',
            ]"
          >
            <!-- 토론 주제 -->
            <CardHeader class="pb-4 transition-all duration-700 ease-in-out">
              <CardTitle
                class="text-2xl font-bold text-center"
                :class="isDebateInfoCollapsed ? 'opacity-0' : 'opacity-100'"
                >{{ debateSubject }}</CardTitle
              >
            </CardHeader>

            <!-- 현재 토론 단계 -->
            <Card
              class="text-center mb-4 transition-all duration-700 ease-in-out p-4"
              style="transition-delay: 100ms"
            >
              <CardContent v-if="!isTransitionStarted">
                <h3 class="text-sm font-medium mb-2">현재 단계</h3>
                <div class="text-lg font-bold text-blue-600">
                  {{ currentStage }}
                </div>
                <div class="flex justify-center mt-2 space-x-1">
                  <div
                    v-for="(stage, index) in stages"
                    :key="stage"
                    :class="[
                      'w-2 h-2 rounded-full transition-all duration-300',
                      index <= currentStageIndex
                        ? 'bg-blue-500'
                        : 'bg-gray-300',
                    ]"
                  ></div>
                </div>
              </CardContent>
              <CardContent v-else>
                <div class="text-center">
                  <!-- 설명 텍스트 -->
                  <div class="text-sm font-medium mb-2">
                    <template v-if="nextSpeaker !== ''">
                      다음 발언자는
                      {{
                        roomStore.leftTeam.find((p) => p.userId === nextSpeaker)
                          ?.displayName ||
                        roomStore.rightTeam.find(
                          (p) => p.userId === nextSpeaker
                        )?.displayName ||
                        nextSpeaker
                      }}입니다.
                    </template>
                    <template v-else>
                      대기시간
                    </template>
                  </div>

                  <!-- 메인 카운트다운 숫자 -->
                  <div class="text-lg font-bold text-blue-600 mb-2">
                    {{ speakingTransitionTimeLeft || 0 }}초
                  </div>

                  <!-- 프로그레스 바 -->
                  <div class="w-96 bg-gray-700 rounded-full h-3 mx-auto">
                    <div
                      class="bg-blue-500 h-3 rounded-full transition-all duration-1000 ease-linear"
                      :style="{
                        width: `${((speakingTransitionTimeLeft || 0) / (3000 / 1000)) * 100}%`,
                      }"
                    ></div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <!-- 토론 정보 그리드 -->
            <div
              class="grid grid-cols-2 gap-6 transition-all duration-700 ease-in-out"
              style="transition-delay: 200ms"
            >
              <!-- 좌측 진영 정보 -->
              <Card
                class="bg-[hsl(var(--debate-left-deep))] transition-all duration-700 ease-in-out border-debate-left text-white gap-2"
                style="transition-delay: 300ms"
              >
                <CardHeader>
                  <CardTitle class="text-2xl text-center px-4">{{
                    debateLeftTeam
                  }}</CardTitle>
                </CardHeader>
                <CardContent class="space-y-2">
                  <div class="flex flex-row justify-around gap-2">
                    <div
                      v-for="participant in roomStore.leftTeam"
                      :key="participant.userId"
                      class="flex flex-col items-center space-y-1"
                    >
                      <Popover>
                        <PopoverTrigger as-child>
                          <Avatar
                            :class="[
                              'w-16 h-16 relative overflow-visible cursor-pointer',
                              (participant.userId === debateStore.myEmail
                                ? audioControls.getLocalSpeaking()
                                : audioControls.getParticipantSpeaking(participant.userId))
                                ? 'speaking-glow'
                                : '',
                            ]"
                          >
                            <AvatarImage
                              :src="debateLeftProfile"
                              :alt="participant.displayName"
                              class="border-2 border-black rounded-full bg-white"
                            />
                            <!-- 공격/수비 아이콘 -->
                            <div
                              v-if="participantStates[participant.userId]"
                              class="absolute -top-8 z-10 w-16 h-16 flex items-center justify-center"
                            >
                              <img
                                :src="
                                  participantStates[participant.userId] ===
                                  'attack'
                                    ? attackIcon
                                    : defenseIcon
                                "
                                :alt="
                                  participantStates[participant.userId] ===
                                  'attack'
                                    ? '공격'
                                    : '수비'
                                "
                                class="w-8 h-8"
                              />
                            </div>
                            <!-- 음소거 배지 -->
                            <div
                              v-if="isParticipantMuted(participant.userId)"
                              class="absolute -bottom-1 -right-1 w-6 h-6 rounded-full border-2 border-black bg-red-500 text-white text-[10px] flex items-center justify-center"
                              title="음소거됨"
                            >
                              🔇
                            </div>
                          </Avatar>
                        </PopoverTrigger>
                        <PopoverContent
                          class="w-72 bg-white text-black shadow-lg border border-gray-200"
                        >
                          <div class="space-y-4">
                            <div class="flex items-center justify-between">
                              <Label class="text-sm">음소거</Label>
                              <Switch
                                :modelValue="
                                  audioControls.getParticipantMuted(
                                    participant.userId
                                  )
                                "
                                @update:modelValue="
                                  (v: boolean) =>
                                    audioControls.setParticipantMuted(
                                      participant.userId,
                                      v
                                    )
                                "
                                class="border-gray-200"
                              />
                            </div>
                            <div
                              v-if="participant.userId === debateStore.myEmail"
                              class="flex items-center justify-between"
                            >
                              <Label class="text-sm">음성 변조</Label>
                              <Switch
                                :modelValue="
                                  audioControls.isVoiceModulated.value
                                "
                                @update:modelValue="onToggleVoiceMod"
                                class="border-gray-200"
                              />
                            </div>
                            <div class="space-y-2">
                              <div class="flex items-center justify-between">
                                <Label class="text-sm">볼륨</Label>
                                <span class="text-xs text-gray-600 font-mono"
                                  >{{
                                    Math.round(
                                      audioControls.getParticipantVolume(
                                        participant.userId
                                      ) * 100
                                    )
                                  }}%</span
                                >
                              </div>
                              <Slider
                                :min="0"
                                :max="1"
                                :step="0.01"
                                :modelValue="[
                                  audioControls.getParticipantVolume(
                                    participant.userId
                                  ),
                                ]"
                                @update:modelValue="
                                  (vals?: number[]) =>
                                    vals &&
                                    audioControls.setParticipantVolume(
                                      participant.userId,
                                      vals[0]
                                    )
                                "
                                class="bg-gray-200 rounded-full"
                              />
                            </div>
                          </div>
                        </PopoverContent>
                      </Popover>
                      <span
                        class="text-xs font-medium text-center break-words max-w-full h-[2.6em] flex leading-tight justify-center items-center"
                        style="
                          word-break: keep-all;
                          overflow-wrap: break-word;
                          line-height: 1.2;
                          white-space: pre-line;
                        "
                      >
                        {{
                          participant.displayName.length > 8
                            ? participant.displayName.slice(0, 8) +
                              "\n" +
                              participant.displayName.slice(8)
                            : participant.displayName
                        }}
                      </span>
                    </div>
                  </div>
                  <div class="mt-3 text-center">
                    <div class="text-xs">발언 시간</div>
                    <div class="text-lg font-bold">
                      {{ isLeftSpeaking ? speakingTimeLeft || 0 : 0 }}초
                    </div>
                    <!-- 프로그레스 바 -->
                    <div class="w-full bg-white rounded-full h-3 mx-auto">
                      <div
                        class="bg-blue-500 h-3 rounded-full transition-all duration-1000 ease-linear"
                        :style="{
                          width: isLeftSpeaking
                            ? `${((speakingTimeLeft || 0) / (speakingDuration / 1000)) * 100}%`
                            : '0%',
                        }"
                      ></div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <!-- 우측 진영 정보 -->
              <Card
                class="bg-[hsl(var(--debate-right-deep))] transition-all duration-700 ease-in-out border-debate-right text-white gap-2"
                style="transition-delay: 300ms"
              >
                <CardHeader class="p-0">
                  <CardTitle class="text-2xl text-center px-4">{{
                    debateRightTeam
                  }}</CardTitle>
                </CardHeader>
                <CardContent class="space-y-2">
                  <div class="flex flex-row justify-around gap-2">
                    <div
                      v-for="participant in roomStore.rightTeam"
                      :key="participant.userId"
                      class="flex flex-col items-center space-y-1"
                    >
                      <Popover>
                        <PopoverTrigger as-child>
                          <Avatar
                            :class="[
                              'w-16 h-16 relative overflow-visible cursor-pointer',
                              (participant.userId === debateStore.myEmail
                                ? audioControls.getLocalSpeaking()
                                : audioControls.getParticipantSpeaking(participant.userId))
                                ? 'speaking-glow'
                                : '',
                            ]"
                          >
                            <AvatarImage
                              :src="debateRightProfile"
                              :alt="participant.displayName"
                              class="border-2 border-black rounded-full bg-white"
                            />
                            <!-- 공격/수비 아이콘 -->
                            <div
                              v-if="participantStates[participant.userId]"
                              class="absolute -top-8 z-10 w-16 h-16 flex items-center justify-center"
                            >
                              <img
                                :src="
                                  participantStates[participant.userId] ===
                                  'attack'
                                    ? attackIcon
                                    : defenseIcon
                                "
                                :alt="
                                  participantStates[participant.userId] ===
                                  'attack'
                                    ? '공격'
                                    : '수비'
                                "
                                class="w-8 h-8"
                              />
                            </div>
                            <!-- 음소거 배지 -->
                            <div
                              v-if="isParticipantMuted(participant.userId)"
                              class="absolute -bottom-1 -right-1 w-6 h-6 rounded-full border-2 border-black bg-red-500 text-white text-[10px] flex items-center justify-center"
                              title="음소거됨"
                            >
                              🔇
                            </div>
                          </Avatar>
                        </PopoverTrigger>
                        <PopoverContent
                          class="w-72 bg-white text-black shadow-lg border border-gray-200"
                        >
                          <div class="space-y-4">
                            <div class="flex items-center justify-between">
                              <Label class="text-sm">음소거</Label>
                              <Switch
                                :modelValue="
                                  audioControls.getParticipantMuted(
                                    participant.userId
                                  )
                                "
                                @update:modelValue="
                                  (v: boolean) =>
                                    audioControls.setParticipantMuted(
                                      participant.userId,
                                      v
                                    )
                                "
                                class="border-gray-200"
                              />
                            </div>
                            <div
                              v-if="participant.userId === debateStore.myEmail"
                              class="flex items-center justify-between"
                            >
                              <Label class="text-sm">음성 변조</Label>
                              <Switch
                                :modelValue="
                                  audioControls.isVoiceModulated.value
                                "
                                @update:modelValue="onToggleVoiceMod"
                                class="border-gray-200"
                              />
                            </div>
                            <div class="space-y-2">
                              <div class="flex items-center justify-between">
                                <Label class="text-sm">볼륨</Label>
                                <span class="text-xs text-gray-600 font-mono"
                                  >{{
                                    Math.round(
                                      audioControls.getParticipantVolume(
                                        participant.userId
                                      ) * 100
                                    )
                                  }}%</span
                                >
                              </div>
                              <Slider
                                :min="0"
                                :max="1"
                                :step="0.01"
                                :modelValue="[
                                  audioControls.getParticipantVolume(
                                    participant.userId
                                  ),
                                ]"
                                @update:modelValue="
                                  (vals?: number[]) =>
                                    vals &&
                                    audioControls.setParticipantVolume(
                                      participant.userId,
                                      vals[0]
                                    )
                                "
                                class="bg-gray-200 rounded-full"
                              />
                            </div>
                          </div>
                        </PopoverContent>
                      </Popover>
                      <span
                        class="text-xs font-medium text-center break-words max-w-full h-[2.6em] flex leading-tight justify-center items-center"
                        style="
                          word-break: keep-all;
                          overflow-wrap: break-word;
                          line-height: 1.2;
                          white-space: pre-line;
                        "
                      >
                        {{
                          participant.displayName.length > 8
                            ? participant.displayName.slice(0, 8) +
                              "\n" +
                              participant.displayName.slice(8)
                            : participant.displayName
                        }}
                      </span>
                    </div>
                  </div>
                  <div class="mt-3 text-center">
                    <div class="text-xs">발언 시간</div>
                    <div class="text-lg font-bold">
                      {{ isRightSpeaking ? speakingTimeLeft || 0 : 0 }}초
                    </div>
                    <!-- 프로그레스 바 -->
                    <div class="w-full bg-white rounded-full h-3 mx-auto">
                      <div
                        class="bg-blue-500 h-3 rounded-full transition-all duration-1000 ease-linear"
                        :style="{
                          width: isRightSpeaking
                            ? `${((speakingTimeLeft || 0) / (speakingDuration / 1000)) * 100}%`
                            : '0%',
                        }"
                      ></div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </CardContent>

          <!-- 토글 버튼 - 항상 표시되도록 절대 위치 설정 -->
          <div
            :class="[
              'w-full h-12 transition-all duration-500 ease-in-out cursor-pointer group',
              isDebateInfoCollapsed
                ? 'absolute bottom-0 left-0 z-10'
                : 'relative z-10',
            ]"
            @click="toggleDebateInfo"
          >
            <div
              class="w-full h-full flex items-center justify-center transition-all duration-300 ease-in-out group-hover:bg-gray-100 group-hover:bg-opacity-30"
            >
              <svg
                class="w-5 h-5 transition-all duration-500 ease-in-out group-hover:scale-110"
                :class="[isDebateInfoCollapsed ? 'rotate-180' : 'rotate-0']"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M5 15l7-7 7 7"
                />
              </svg>
            </div>
          </div>
        </Card>

        <!-- STT 실시간 토론 내용 영역 (스크롤 가능) -->
        <Card class="flex flex-col min-h-0 bg-white">
          <!-- 현재 발언자 및 대기시간 표시 -->
          <CardContent class="p-0">
            <!-- 메시지 영역 (스크롤 가능) -->
            <div
              ref="messagesContainer"
              id="messages-container"
              class="flex-1 rounded-lg p-4 space-y-6"
            >
              <div
                v-for="message in messages"
                :key="message.id"
                class="relative"
              >
                <!-- 프로필 영역 (양 끝 고정) -->
                <div
                  :class="[
                    'absolute top-0 flex flex-col items-center space-y-2',
                    message.team === 0 ? 'left-0' : 'right-0',
                  ]"
                  style="width: 100px"
                >
                  <!-- STT 메시지 -->
                  <div
                    v-if="message.type === 'stt'"
                    class="flex flex-col items-center"
                  >
                    <Avatar
                      :class="[
                        'w-[min(6vw,64px)] h-[min(6vw,64px)] relative overflow-visible',
                      ]"
                    >
                      <AvatarImage
                        :src="
                          message.team === 0
                            ? debateLeftProfile
                            : debateRightProfile
                        "
                        :alt="message.sender"
                        class="border-2 border-black rounded-full bg-white"
                      />
                      <div
                        v-if="message.mode === 'battle'"
                        class="absolute -top-8 z-10 w-16 h-16 flex items-center justify-center"
                      >
                        <img
                          :src="message.isAttacker ? attackIcon : defenseIcon"
                          :alt="message.isAttacker ? '공격' : '수비'"
                          class="w-6 h-6"
                        />
                      </div>
                    </Avatar>

                    <!-- 발언자 이름 (8글자 넘으면 개행) -->
                    <div
                      class="text-xs font-semibold text-slate-700 text-center leading-tight h-[2.6em] flex items-center justify-center"
                      style="width: 100px"
                    >
                      <span
                        v-if="message.sender && message.sender.length <= 8"
                        >{{ message.sender }}</span
                      >
                      <span v-else-if="message.sender" class="block">
                        {{ message.sender.substring(0, 8) }}<br />{{
                          message.sender.substring(8)
                        }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- 말풍선 영역 (프로필 영역 피해서) -->
                <div v-if="message.type === 'stt'">
                  <div
                    @click="toggleDisplay(message)"
                    :class="[
                      'relative p-8 rounded-4xl text-lg shadow-sm cursor-pointer select-text',
                      message.team === 0
                        ? 'bg-[hsl(var(--debate-left-deep))] text-slate-800 ml-28 mr-28'
                        : 'bg-[hsl(var(--debate-right-deep))] text-white mr-28 ml-28',
                    ]"
                    style="min-height: 80px; display: flex; align-items: center"
                    :title="
                      message.summaryText
                        ? message.display === 'summary'
                          ? 'STT 보기'
                          : '요약 보기'
                        : ''
                    "
                    role="button"
                    :aria-pressed="message.display === 'summary'"
                  >
                    <!-- 말풍선 꼬리 -->
                    <div
                      v-if="message.team === 0"
                      class="absolute top-6 left-[-10px] w-0 h-0 border-t-[12px] border-b-[12px] border-r-[12px] border-t-transparent border-b-transparent border-r-debate-left"
                    ></div>
                    <div
                      v-if="message.team === 1"
                      class="absolute top-6 right-[-10px] w-0 h-0 border-t-[12px] border-b-[12px] border-l-[12px] border-t-transparent border-b-transparent border-l-debate-right"
                    ></div>
                    <div class="whitespace-pre-wrap">
                      {{ displayedText(message) }}
                    </div>

                    <!-- 상태 배지 -->
                    <div class="absolute top-2 right-3 text-xs opacity-80">
                      <span
                        v-if="message.summaryPending"
                        class="px-2 py-0.5 rounded-full bg-black/20"
                        >요약중…</span
                      >
                      <span
                        v-else-if="message.summaryText"
                        class="px-2 py-0.5 rounded-full bg-black/20"
                        >{{
                          message.display === "summary" ? "요약" : "원문"
                        }}</span
                      >
                    </div>
                  </div>
                </div>

                <!-- 투표 결과 -->
                <div v-if="message.type === 'vote'">
                  <div
                    :class="[
                      'relative p-8 rounded-4xl text-lg shadow-sm mx-28 bg-gray-400',
                    ]"
                    style="
                      min-height: 80px;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                    "
                  >
                    <div class="flex flex-col items-center">
                      <h2 class="text-3xl font-bold">투표 결과</h2>
                      <div
                        class="flex flex-row text-center justify-center gap-8 mt-4"
                      >
                        <div class="flex flex-col items-center">
                          <div class="text-4xl font-bold text-debate-left">
                            {{
                              Object.values(message.voteInfo ?? {}).filter(
                                (v) => v === 0
                              ).length
                            }}
                          </div>
                        </div>
                        <div class="text-3xl font-bold self-center">VS</div>
                        <div class="flex flex-col items-center">
                          <div class="text-4xl font-bold text-debate-right">
                            {{
                              Object.values(message.voteInfo ?? {}).filter(
                                (v) => v === 1
                              ).length
                            }}
                          </div>
                        </div>
                      </div>
                      <h3 class="text-center text-2xl font-bold mt-4">
                        {{
                          message.voteResult === 0
                            ? debateLeftTeam + " 승리!"
                            : message.voteResult === 1
                              ? debateRightTeam + " 승리!"
                              : "무승부!"
                        }}
                      </h3>
                      <div
                        class="flex flex-row items-center justify-center mt-4 gap-4"
                      >
                        <div
                          v-for="leftTeam in roomStore.leftTeam"
                          :key="leftTeam.userId"
                          class="flex flex-col items-center"
                        >
                          <Avatar
                            :class="[
                              'w-[min(6vw,64px)] h-[min(6vw,64px)] relative overflow-visible',
                            ]"
                          >
                            <AvatarImage
                              :src="debateLeftProfile"
                              :alt="leftTeam.displayName"
                              class="border-2 border-black rounded-full bg-white"
                            />
                          </Avatar>

                          <!-- 발언자 이름 (8글자 넘으면 개행) -->
                          <div
                            class="text-xs font-semibold text-slate-700 text-center leading-tight h-[2.6em] flex items-center justify-center"
                            style="width: 100px"
                          >
                            <span
                              v-if="
                                leftTeam.displayName &&
                                leftTeam.displayName.length <= 8
                              "
                              >{{ leftTeam.displayName }}</span
                            >
                            <span
                              v-else-if="leftTeam.displayName"
                              class="block"
                            >
                              {{ leftTeam.displayName.substring(0, 8) }}<br />{{
                                leftTeam.displayName.substring(8)
                              }}
                            </span>
                          </div>
                          <!-- 진영색깔 원 -->
                          <div
                            class="w-12 h-12 rounded-full mt-2 flex items-center justify-center shadow-md font-bold border-2 border-black"
                            :class="[
                              message.voteInfo?.[leftTeam.userId] === undefined
                                ? 'bg-red-400'
                                : message.voteInfo?.[leftTeam.userId] === 0
                                  ? 'bg-[hsl(var(--debate-left-deep))]'
                                  : 'bg-[hsl(var(--debate-right-deep))] text-white',
                            ]"
                          >
                            {{
                              message.voteInfo?.[leftTeam.userId] === undefined
                                ? "X"
                                : message.voteInfo?.[leftTeam.userId] === 0
                                  ? "L"
                                  : "R"
                            }}
                          </div>
                        </div>
                      </div>
                      <div
                        class="flex flex-row items-center justify-center mt-4 gap-4"
                      >
                        <div
                          v-for="rightTeam in roomStore.rightTeam"
                          :key="rightTeam.userId"
                          class="flex flex-col items-center"
                        >
                          <Avatar
                            :class="[
                              'w-[min(6vw,64px)] h-[min(6vw,64px)] relative overflow-visible',
                            ]"
                          >
                            <AvatarImage
                              :src="debateRightProfile"
                              :alt="rightTeam.displayName"
                              class="border-2 border-black rounded-full bg-white"
                            />
                          </Avatar>

                          <!-- 발언자 이름 (8글자 넘으면 개행) -->
                          <div
                            class="text-xs font-semibold text-slate-700 text-center leading-tight h-[2.6em] flex items-center justify-center"
                            style="width: 100px"
                          >
                            <span
                              v-if="
                                rightTeam.displayName &&
                                rightTeam.displayName.length <= 8
                              "
                              >{{ rightTeam.displayName }}</span
                            >
                            <span
                              v-else-if="rightTeam.displayName"
                              class="block"
                            >
                              {{ rightTeam.displayName.substring(0, 8)
                              }}<br />{{ rightTeam.displayName.substring(8) }}
                            </span>
                          </div>
                          <!-- 진영색깔 원 -->
                          <div
                            class="w-12 h-12 rounded-full mt-2 flex items-center justify-center shadow-md font-bold border-2 border-black"
                            :class="[
                              message.voteInfo?.[rightTeam.userId] === undefined
                                ? 'bg-red-400'
                                : message.voteInfo?.[rightTeam.userId] === 0
                                  ? 'bg-[hsl(var(--debate-left-deep))]'
                                  : 'bg-[hsl(var(--debate-right-deep))] text-white',
                            ]"
                          >
                            {{
                              message.voteInfo?.[rightTeam.userId] === undefined
                                ? "X"
                                : message.voteInfo?.[rightTeam.userId] === 0
                                  ? "L"
                                  : "R"
                            }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- AI 판정단 결과 -->
                <div v-if="message.type === 'ai' && isTie">
                  <div
                    :class="[
                      'relative p-8 rounded-4xl text-lg shadow-sm mx-28 bg-gray-400',
                    ]"
                  >
                    <div class="flex flex-col items-center">
                      <h2 class="text-3xl font-bold text-gray-800">
                        AI 판정단 결과
                      </h2>
                      <div v-if="isLoadingAIMessage">
                        <!-- 로딩 점들 -->
                        <div
                          class="flex space-x-2 justify-center items-center my-20"
                        >
                          <div
                            class="w-3 h-3 bg-gray-600 rounded-full animate-bounce"
                          ></div>
                          <div
                            class="w-3 h-3 bg-gray-600 rounded-full animate-bounce"
                            style="animation-delay: 0.1s"
                          ></div>
                          <div
                            class="w-3 h-3 bg-gray-600 rounded-full animate-bounce"
                            style="animation-delay: 0.2s"
                          ></div>
                        </div>

                        <p class="text-gray-700 mt-4 text-center">
                          AI가 토론 내용을 분석하고 있습니다...
                        </p>
                      </div>
                      <div v-if="!isLoadingAIMessage">
                        <!-- 좌/우 점수 -->
                        <div
                          class="flex items-center justify-center space-x-16 mt-6"
                        >
                          <div class="text-6xl font-bold text-debate-left">
                            {{ message.aiInfo?.L }}
                          </div>
                          <div class="text-6xl font-bold text-debate-right">
                            {{ message.aiInfo?.R }}
                          </div>
                        </div>

                        <!-- 블록 그리드 -->
                        <div
                          class="flex items-start justify-center space-x-12 mt-6"
                        >
                          <div class="grid grid-cols-5 gap-2">
                            <div
                              v-for="n in message.aiInfo?.L"
                              :key="`ai-left-${n}`"
                              class="w-4 h-4 bg-[hsl(var(--debate-left-deep))] rounded-sm shadow-md"
                            ></div>
                          </div>
                          <div class="grid grid-cols-5 gap-2">
                            <div
                              v-for="n in message.aiInfo?.R"
                              :key="`ai-right-${n}`"
                              class="w-4 h-4 bg-[hsl(var(--debate-right-deep))] rounded-sm shadow-md"
                            ></div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 메시지가 없을 때 -->
              <div
                v-if="messages.length === 0"
                class="text-center text-slate-500 py-12"
              >
                <div class="text-lg mb-2">🎤</div>
                <div>토론이 시작되면 실시간 발언 내용이 여기에 표시됩니다.</div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- 공격 대상 선택 모달 -->
      <Dialog
        :open="isSelectTarget"
        @update:open="handleSelectTargetModalClose"
      >
        <DialogContent class="z-51 w-[1000px] max-w-none">
          <DialogHeader>
            <DialogTitle class="text-center">공격 대상 선택</DialogTitle>
          </DialogHeader>
          <!-- 상대편 목록 -->
          <div class="space-y-3 max-h-60 overflow-y-auto">
            <!-- 좌측 진영 (상대편인 경우) -->
            <div v-if="debateStore.myTeam === 'R'" class="space-y-2">
              <div
                v-for="participant in roomStore.leftTeam"
                :key="participant.userId"
                @click="selectTargetParticipant(participant)"
                :class="[
                  'flex items-center space-x-3 p-3 rounded-lg cursor-pointer transition-all duration-200 border-2 mt-2',
                  selectedCurrentTarget?.userId === participant.userId
                    ? 'shadow-sm bg-[hsl(var(--debate-left-deep))] text-white border-black'
                    : 'border-black hover:bg-[hsl(var(--debate-left-deep))] hover:text-white hover:shadow-md hover:-translate-y-0.5',
                ]"
              >
              <div class="flex flex-col items-center">
                  <div>
                    <Avatar :class="['w-10 h-10']">
                      <AvatarImage
                        :src="debateLeftProfile"
                        :alt="participant.displayName"
                        class="border-2 border-black rounded-full bg-white"
                      />
                    </Avatar>
                  </div>
                  <div
                    class="text-xs font-semibold text-slate-700 text-center leading-tight h-[2.6em] flex items-center justify-center"
                    style="width: 100px"
                  >
                    <span
                      v-if="
                        participant.displayName &&
                        participant.displayName.length <= 8
                      "
                      >{{ participant.displayName }}</span
                    >
                    <span v-else-if="participant.displayName" class="block">
                      {{ participant.displayName.substring(0, 8) }}<br />{{
                        participant.displayName.substring(8)
                      }}
                    </span>
                  </div>
                </div>
                <div class="font-bold">
                  {{ messages.filter(m => m.sender === participant.displayName).slice(-1)[0]?.summaryText || '요약 없음' }}
                </div>
                <div
                  v-if="
                    selectedCurrentTarget?.userId === participant.userId &&
                    selectTargetConfirmed
                  "
                  class="text-white"
                >
                  <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                    <path
                      fill-rule="evenodd"
                      d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                      clip-rule="evenodd"
                    />
                  </svg>
                </div>
                <div
                  v-if="
                    selectedCurrentTarget?.userId === participant.userId &&
                    !selectTargetConfirmed
                  "
                  class="text-white"
                >
                  <div
                    class="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"
                  ></div>
                </div>
              </div>
            </div>

            <!-- 우측 진영 (상대편인 경우) -->
            <div v-if="debateStore.myTeam === 'L'" class="space-y-2">
              <div
                v-for="participant in roomStore.rightTeam"
                :key="participant.userId"
                @click="selectTargetParticipant(participant)"
                :class="[
                  'flex items-center space-x-3 p-3 rounded-lg cursor-pointer transition-all duration-200 border-2 mt-2',
                  selectedCurrentTarget?.userId === participant.userId
                    ? 'shadow-sm bg-[hsl(var(--debate-right-deep))] text-white border-black'
                    : 'border-black hover:bg-[hsl(var(--debate-right-deep))] hover:text-white hover:shadow-md hover:-translate-y-0.5',
                ]"
              >
                <div class="flex flex-col items-center">
                  <div>
                    <Avatar :class="['w-10 h-10']">
                      <AvatarImage
                        :src="debateRightProfile"
                        :alt="participant.displayName"
                        class="border-2 border-black rounded-full bg-white"
                      />
                    </Avatar>
                  </div>
                  <div
                    class="text-xs font-semibold text-slate-700 text-center leading-tight h-[2.6em] flex items-center justify-center"
                    style="width: 100px"
                  >
                    <span
                      v-if="
                        participant.displayName &&
                        participant.displayName.length <= 8
                      "
                      >{{ participant.displayName }}</span
                    >
                    <span v-else-if="participant.displayName" class="block">
                      {{ participant.displayName.substring(0, 8) }}<br />{{
                        participant.displayName.substring(8)
                      }}
                    </span>
                  </div>
                </div>
                <div class="font-bold">
                  {{ messages.filter(m => m.sender === participant.displayName).slice(-1)[0]?.summaryText || '요약 없음' }}
                </div>
                <div
                  v-if="
                    selectedCurrentTarget?.userId === participant.userId &&
                    selectTargetConfirmed
                  "
                  class="text-white"
                >
                  <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                    <path
                      fill-rule="evenodd"
                      d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                      clip-rule="evenodd"
                    />
                  </svg>
                </div>
                <div
                  v-if="
                    selectedCurrentTarget?.userId === participant.userId &&
                    !selectTargetConfirmed
                  "
                  class="text-white"
                >
                  <div
                    class="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <!-- 실시간 공격 대상 표시 -->
          <h3 class="text-lg font-bold text-center">대결 상대</h3>
          <div class="grid gap-y-6">
            <div
              v-for="(row, i) in selectedRows"
              :key="i"
              class="grid grid-cols-2 gap-6 items-center justify-center min-w-0"
            >
              <!-- 왼쪽 셀 -->
              <div class="grid grid-cols-4 items-center min-w-0">
                <template v-if="row.left">
                  <!-- 아바타 -->
                  <div
                    class="col-span-1 grid grid-cols-1 items-center justify-items-center"
                  >
                    <Avatar
                      :class="[
                        'w-16 h-16 overflow-visible',
                      ]"
                    >
                      <AvatarImage
                        :src="debateLeftProfile"
                        :alt="row.left.displayName"
                        class="border-2 border-black rounded-full"
                      />
                    </Avatar>
                    <div
                      class="text-xs font-medium text-center break-words whitespace-pre-line leading-tight max-w-full h-[2.6em] flex items-center justify-center"
                    >
                      {{ wrapName(row.left.displayName) }}
                    </div>
                  </div>
                  <!-- 공격자 목록 -->
                  <div class="col-span-3 min-w-0">
                    <div
                      v-for="attacker in attackersFor(row.left.userId)"
                      :key="attacker"
                      class="flex items-center justify-start gap-4 m-2"
                    >
                      <img :src="attackIcon" alt="공격" class="w-6 h-6" />
                      <span
                        class="flex-1 min-w-0 text-left font-bold break-words whitespace-pre-line leading-tight max-w-full"
                      >
                        {{
                          wrapName(
                            roomStore.rightTeam.find(
                              (p) => p.userId === attacker
                            )?.displayName || ""
                          )
                        }}
                      </span>
                    </div>
                  </div>
                </template>
              </div>
              <!-- 오른쪽 셀 -->
              <div class="grid grid-cols-4 items-center min-w-0">
                <template v-if="row.right">
                  <!-- 공격자 목록 -->
                  <div class="col-span-3 min-w-0">
                    <div
                      v-for="attacker in attackersFor(row.right.userId)"
                      :key="attacker"
                      class="flex items-center justify-end gap-4 m-2"
                    >
                      <span
                        class="flex-1 min-w-0 text-right font-bold break-words whitespace-pre-line leading-tight max-w-full"
                      >
                        {{
                          wrapName(
                            roomStore.leftTeam.find(
                              (p) => p.userId === attacker
                            )?.displayName || ""
                          )
                        }}
                      </span>
                      <img :src="attackIcon" alt="공격" class="w-6 h-6" />
                    </div>
                  </div>
                  <!-- 아바타 -->
                  <div
                    class="col-span-1 grid grid-cols-1 items-center justify-items-center"
                  >
                    <Avatar
                      :class="[
                        'w-16 h-16 overflow-visible',
                      ]"
                    >
                      <AvatarImage
                        :src="debateRightProfile"
                        :alt="row.right.displayName"
                        class="border-2 border-black rounded-full"
                      />
                    </Avatar>
                    <div
                      class="text-xs font-medium text-center break-words whitespace-pre-line leading-tight max-w-full h-[2.6em] flex items-center justify-center"
                    >
                      {{ wrapName(row.right.displayName) }}
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>

          <!-- 공격 대상 선택 시간 -->
          <DialogFooter class="mt-2">
            <div class="text-center w-full">
              <div class="text-xs">남은 시간</div>
              <div
                class="text-lg font-bold"
                :class="[
                  (selectTargetTimeLeft || 0) <= 10 ? 'text-red-600 blink' : '',
                ]"
              >
                {{ selectTargetTimeLeft || 0 }}초
              </div>
              <!-- 프로그레스 바 -->
              <div class="w-full bg-gray-800 rounded-full h-3 mx-auto">
                <div
                  :class="[
                    'h-3 rounded-full transition-all duration-1000 ease-linear',
                    (selectTargetTimeLeft || 0) <= 10
                      ? 'bg-red-500'
                      : 'bg-blue-500',
                  ]"
                  :style="{
                    width: `${((selectTargetTimeLeft || 0) / (SELECT_TARGET_DURATION / 1000)) * 100}%`,
                  }"
                ></div>
              </div>
            </div>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <!-- 투표 모달 -->
      <Dialog :open="isVoteTime" @update:open="handleVoteModalClose">
        <DialogContent class="z-51 w-[1000px] max-w-none">
          <DialogHeader>
            <DialogTitle class="text-center text-3xl">최종 투표</DialogTitle>
            <DialogTitle class="text-center text-2xl py-2">
              {{ debateSubject }}
            </DialogTitle>
          </DialogHeader>
          <div class="grid grid-cols-2 gap-4 text-center">
            <Button
              variant="outline"
              class="w-full h-full hover:bg-blue-500 rounded-xl bg-[hsl(var(--debate-left-deep))] text-white hover:-translate-y-0.5 transition-all duration-300"
              @click="vote(debateStore.myEmail, 0)"
              :disabled="isVoteDisabled"
              :class="[
                voteTeam === 0
                  ? 'bg-[hsl(var(--debate-left-deep))]'
                  : 'bg-gray-300',
              ]"
            >
              <Card class="border-none shadow-none">
                <CardContent class="flex items-center justify-center h-full">
                  <div class="text-2xl">{{ debateLeftTeam }}</div>
                </CardContent>
              </Card>
            </Button>
            <Button
              variant="outline"
              class="w-full h-full hover:bg-blue-500 rounded-xl bg-[hsl(var(--debate-right-deep))] text-white hover:-translate-y-0.5 transition-all duration-300"
              @click="vote(debateStore.myEmail, 1)"
              :disabled="isVoteDisabled"
              :class="[
                voteTeam === 1
                  ? 'bg-[hsl(var(--debate-right-deep))]'
                  : 'bg-gray-300',
              ]"
            >
              <Card class="border-none shadow-none">
                <CardContent class="flex items-center justify-center h-full">
                  <div class="text-2xl">{{ debateRightTeam }}</div>
                </CardContent>
              </Card>
            </Button>
          </div>

          <!-- 선택 시간 -->
          <DialogFooter class="mt-2">
            <div class="text-center w-full">
              <div class="text-xs">남은 시간</div>
              <div
                class="text-lg font-bold"
                :class="[(voteTimeLeft || 0) <= 10 ? 'text-red-600 blink' : '']"
              >
                {{ voteTimeLeft || 0 }}초
              </div>
              <!-- 프로그레스 바 -->
              <div class="w-full bg-gray-800 rounded-full h-3 mx-auto">
                <div
                  :class="[
                    'h-3 rounded-full transition-all duration-1000 ease-linear',
                    (voteTimeLeft || 0) <= 10 ? 'bg-red-500' : 'bg-blue-500',
                  ]"
                  :style="{
                    width: `${((voteTimeLeft || 0) / (VOTE_DURATION / 1000)) * 100}%`,
                  }"
                ></div>
              </div>
            </div>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <!-- 참가자별 오디오 엘리먼트들 (숨김) -->
      <div style="display: none">
        <div
          v-for="participant in state.participants"
          :key="participant.producerUserEmail"
        >
          <audio
            :ref="
              (el) => setParticipantAudioRef(participant.producerUserEmail, el)
            "
            autoplay
            playsinline
          ></audio>
        </div>
      </div>

      <!-- 오디오 연결 상태 (디버깅용 - 개발 중에만 표시) -->
      <div class="p-3 bg-blue-50 border border-blue-200 rounded-lg mb-4 hidden">
        <h4 class="text-xs font-semibold text-blue-800 mb-2">
          🔊 오디오 연결 상태
        </h4>
        <div class="space-y-1">
          <div
            v-for="participant in state.participants"
            :key="participant.producerUserEmail"
            class="flex items-center justify-between text-xs"
          >
            <span class="text-blue-700 font-mono">{{
              participant.producerUserEmail
            }}</span>
            <div class="flex items-center space-x-2">
              <span
                :class="[
                  'px-2 py-0.5 rounded text-xs font-medium',
                  participant.connected
                    ? 'bg-green-100 text-green-700'
                    : 'bg-red-100 text-red-700',
                ]"
              >
                {{ participant.connected ? "🟢 연결됨" : "🔴 대기중" }}
              </span>
              <span
                v-if="participant.audioStream"
                class="text-green-600 text-xs"
                >🎵</span
              >
            </div>
          </div>
          <div
            v-if="state.participants.length === 0"
            class="text-xs text-gray-500 italic"
          >
            참가자 대기 중...
          </div>
        </div>
      </div>

      <!-- 우측: 시청자 채팅창 -->
      <div
        class="pt-6 px-6 pl-3"
        style="flex: 1; min-width: 320px; max-width: 640px"
      >
        <div class="sticky top-12" style="height: calc(100vh - 120px)">
          <!-- 시청자 채팅창 -->
          <Card class="h-full flex flex-col bg-white">
            <!-- 시청자 채팅 헤더 -->
            <CardHeader
              class="flex flex-row items-center justify-between space-y-0 pb-3"
            >
              <CardTitle class="text-lg">채팅창</CardTitle>
              <Badge variant="secondary" class="flex items-center space-x-1">
                <div class="w-2 h-2 bg-green-500 rounded-full"></div>
                <span class="text-xs">{{ audienceCount }}명</span>
              </Badge>
            </CardHeader>

            <!-- 시청자 채팅 메시지 영역 -->
            <CardContent class="flex-1 flex flex-col px-1 min-h-0">
              <ScrollArea
                ref="audienceChatScrollArea"
                class="flex-1 rounded-lg px-3 pt-2 overflow-y-auto"
                type="always"
              >
                <div ref="audienceChatContainer" class="space-y-4">
                  <div
                    v-for="chatMessage in audienceMessages"
                    :key="chatMessage.id"
                    class="flex space-x-1 items-start"
                  >
                    <!-- 왼쪽: 시청자 아바타 이미지 -->
                    <Avatar class="w-12 h-12 mt-2">
                      <AvatarImage
                        :src="audienceProfile"
                        alt="시청자"
                        class="border-2 border-black rounded-full"
                      />
                    </Avatar>

                    <!-- 오른쪽: 닉네임과 메시지 -->
                    <div class="flex-1 flex flex-col">
                      <!-- 닉네임 -->
                      <Badge
                        class="font-bold truncate max-w-fit self-start text-lg py-0"
                        :title="chatMessage.nickname"
                      >
                        {{ chatMessage.nickname }}
                      </Badge>

                      <!-- 메시지 -->
                      <Card class="p-0 border-none shadow-none">
                        <CardContent class="py-0 px-2">
                          <div class="">
                            {{ chatMessage.text }}
                          </div>
                        </CardContent>
                      </Card>
                    </div>
                  </div>

                  <!-- 채팅이 없을 때 -->
                  <div
                    v-if="audienceMessages.length === 0"
                    class="text-center text-slate-400 py-8"
                  >
                    <div class="text-sm mb-1">💬</div>
                    <div class="text-xs">아직 채팅이 없습니다</div>
                  </div>
                </div>
              </ScrollArea>
            </CardContent>

            <!-- 채팅 입력 영역 (관리자용 또는 테스트용) -->
            <CardFooter class="pb-0 px-6">
              <div class="flex space-x-2 w-full">
                <Input
                  v-model="newChatMessage"
                  @keyup.enter="sendChatMessage"
                  placeholder="채팅을 입력하세요..."
                  class="flex-1 text-xs"
                />
                <Button @click="sendChatMessage" size="sm" class="text-xs">
                  전송
                </Button>
              </div>
            </CardFooter>
          </Card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  onMounted,
  onUnmounted,
  nextTick,
  watch,
  computed,
  onBeforeMount,
  watchEffect,
} from "vue";
import { useRoute } from "vue-router";
import debateLeftProfile from "@/assets/images/profile/debate_left.png";
import debateRightProfile from "@/assets/images/profile/debate_right.png";
import audienceProfile from "@/assets/images/profile/debate_random.png";
import attackIcon from "@/assets/images/icons/attack.png";
import defenseIcon from "@/assets/images/icons/defense.png";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardFooter,
} from "@/components/ui/card";
import { Avatar, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { useAuthStore } from "@/store/auth";
import { useDebateStore } from "@/store/useDebateStore";
import type {
  Team,
  Speaker,
  DebateSession,
  SpeakerChangeMessage,
  STTMessage,
  SessionUpdateMessage,
  DebateStartMessage,
  DebateSpeakStartMessage,
  DebateSpeakEndMessage,
  DebateSelectTargetMessage,
  DebateSelectTargetResponse,
  DebateVoteStartMessage,
  DebateVoteEndMessage,
} from "@/types/debate";
import {
  useWebRTCConnection,
  type StompClient,
} from "@/composables/useWebRTCConnection";
import { useAudioControls } from "@/composables/useAudioControls";
import { useRoomStore } from "@/store/roomStore";
import { Client } from "@stomp/stompjs";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { useTargetSelectionStore } from "@/store/targetSelection";
import { useStt } from "@/composables/useSpeechRecognition";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Slider } from "@/components/ui/slider";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";

const client = ref<Client | null>(null);
const route = useRoute();

// Debate Store 사용
const debateStore = useDebateStore();
const roomStore = useRoomStore();
const authStore = useAuthStore();
const targetSelectionStore = useTargetSelectionStore();
const audioControls = useAudioControls();

const roomIdParams = route.query.roomId as string;
console.log("roomIdParams", roomIdParams);

// URL 파라미터에서 사용자 정보 가져오기
// 예시 URL: /debate-room/11?roomId=11&userEmail=user01@test.com&side=L&isModerator=true
const currentUserEmail = route.query.userEmail as string;
const currentUserSide = (route.query.side as string) || "L";
const isModerator = route.query.isModerator === "true" || false;

console.log("currentUserEmail", currentUserEmail);
console.log("currentUserSide", currentUserSide);
console.log("isModerator", isModerator);

// debate 스토어에 정보 설정
debateStore.setRoomId(roomIdParams);
debateStore.setMyInfo(
  currentUserEmail,
  currentUserSide as "L" | "R",
  isModerator
);

// @stomp/stompjs Client를 StompClient 인터페이스에 맞게 래핑
const createStompClientWrapper = (client: any): StompClient | undefined => {
  if (!client) return undefined;

  return {
    connected: client.connected,
    connect: (url: string, token: string, userEmail?: string) => {
      // 이미 연결된 경우 아무것도 하지 않음
    },
    disconnect: () => client.deactivate(),
    subscribe: (destination: string, callback: (message: any) => void) => {
      const subscription = client.subscribe(destination, callback);
      return {
        unsubscribe: () => subscription.unsubscribe(),
      };
    },
    publish: (destination: string, body: any) => {
      client.publish({
        destination,
        body: typeof body === "string" ? body : JSON.stringify(body),
      });
    },
  };
};

const getAuthToken = async () => {
  const response = await fetch(
    `${import.meta.env.VITE_API_BASE_URL}/api/auth/token`,
    {
      method: "POST",
      mode: "cors",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({ userEmail: currentUserEmail }),
    }
  );

  const data = await response.json();
  const authToken = data.token;

  return authToken;
};

// targetSelectionStore.applySelect({
//   attacker: "user01@test.com",
//   defender: "user02@test.com",
// });

// targetSelectionStore.applySelect({
//   attacker: "user02@test.com",
//   defender: "user01@test.com",
// });

// targetSelectionStore.applySelect({
//   attacker: "user04@test.com",
//   defender: "user01@test.com",
// });
// targetSelectionStore.applySelect({
//   attacker: "user03@test.com",
//   defender: "user04@test.com",
// });

// 참가자별 오디오 엘리먼트 참조 설정
const setParticipantAudioRef = (userEmail: string, el: any) => {
  if (el && el instanceof HTMLAudioElement) {
    audioControls.setParticipantAudio(userEmail, el);
    console.log(`🎵 ${userEmail}의 오디오 엘리먼트 설정됨`);
  }
};

// 프로필 음소거 표시 조건: 음소거 상태이거나 볼륨이 0
const isParticipantMuted = (userId: string) => {
  const muted = audioControls.getParticipantMuted(userId);
  const volume = audioControls.getParticipantVolume(userId);
  return muted || volume <= 0;
};

// 내 프로필 팝오버에서 음성 변조 토글 핸들러
const onToggleVoiceMod = async (next: boolean) => {
  try {
    if (next !== audioControls.isVoiceModulated.value) {
      await audioControls.toggleVoiceModulation();
    }
    // 변조 적용 후 현재 로컬 트랙을 WebRTC Producer에 반영
    if (audioControls.localAudioTrack.value) {
      await replaceLocalAudioTrack(audioControls.localAudioTrack.value);
    }
  } catch (e) {
    console.error("음성 변조 토글 실패:", e);
  }
};

// 테스트용 참가자 데이터 추가가
roomStore.setRoom({
  roomId: roomIdParams,
  participants: [
    { userId: "user01@test.com", displayName: "김정택", side: "L" },
    {
      userId: "user02@test.com",
      displayName: "권우상권우상권우권우상권우상권우",
      side: "R",
    },
    // { userId: "user03@test.com", displayName: "김형수", side: "L" },
    // { userId: "user04@test.com", displayName: "지준오", side: "R" },
  ],
  topic: "인간은 성선설인가 성악설인가?",
  leftTeamName: "성선설",
  rightTeamName: "성악설",
  mode: "0",
});

// STT 관련
const msgIndexByTurn = new Map<string, number>();
const opinionQueueByUser = new Map<string, string[]>();
const battleQueueByUser = new Map<string, string[]>();

function attachSummaryByQueue(
  user: string,
  summary: string,
  mode: "normal" | "battle"
) {
  const queueMap = mode === "battle" ? battleQueueByUser : opinionQueueByUser;
  const q = queueMap.get(user) ?? [];
  const turnKey = q.shift(); // 가장 오래된 미해결 턴
  queueMap.set(user, q);

  if (!turnKey) return; // 붙일 대상 없음

  const idx = msgIndexByTurn.get(turnKey);
  if (idx == null) return;

  const m = messages.value[idx];
  m.summaryText = summary;
  m.summaryPending = false;
  m.display = "summary"; // 요약 먼저 보여주고 싶으면
}

const makeTurnKey = (user: string, startedAt: string) => {
  return `${user}|${startedAt}`;
};

const stt = useStt(client, roomStore.room?.roomId ?? 0, {
  lang: "ko-KR",
});

stt.onSttMessage((payload: any) => {
  const body = payload?.data?.text ? payload.data : payload;
  const user = body?.user ?? "-";
  const text = body?.text ?? JSON.stringify(payload);
  const now = new Date();

  const turnKey = makeTurnKey(user, speakerStartAt.value);

  const messageMode: "normal" | "battle" | undefined =
    currentStageIndex.value < 2
      ? "normal"
      : currentStageIndex.value < 4
        ? "battle"
        : undefined;

  let idx = msgIndexByTurn.get(turnKey);

  if (idx == null) {
    const side = roomStore.room?.participants.find(
      (p) => p.userId === user
    )?.side;
    const team = side === "L" ? 0 : side === "R" ? 1 : undefined;

    messages.value.push({
      id: messageIdCounter++,
      type: "stt",
      mode: messageMode,
      team,
      timestamp: now,
      sender: user,
      sttText: "",
      isAttacker:
        currentStageIndex.value >= 2 &&
        currentSpeaker.value === user &&
        isAttackPhase.value
          ? true
          : undefined,
      isDefender:
        currentStageIndex.value >= 2 &&
        currentSpeaker.value === user &&
        isDefensePhase.value
          ? true
          : undefined,
      summaryPending: true,
      display: "stt",
      turnKey,
    });
    idx = messages.value.length - 1;
    msgIndexByTurn.set(turnKey, idx);

    // 요약 대기 큐에 등록 (모드별로)
    const q =
      messageMode === "battle"
        ? (battleQueueByUser.get(user) ?? [])
        : (opinionQueueByUser.get(user) ?? []);
    q.push(turnKey);
    if (messageMode === "battle") battleQueueByUser.set(user, q);
    else opinionQueueByUser.set(user, q);
  }

  // 3) 텍스트 누적
  const m = messages.value[idx];
  m.sttText = [m.sttText, text].filter(Boolean).join(" ");
  m.timestamp = now;

  // 4) 메모리 관리
  const LIMIT = 200;
  if (messages.value.length > LIMIT) {
    const removed = messages.value.shift(); // 가장 오래된 것 제거
    if (removed?.turnKey) {
      // 맵/큐 정리
      msgIndexByTurn.delete(removed.turnKey);
      // 큐 내부에서도 제거
      const removeFromQueue = (q: Map<string, string[]>) => {
        const arr = q.get(removed.sender ?? "");
        if (!arr) return;
        const i = arr.indexOf(removed.turnKey!);
        if (i >= 0) arr.splice(i, 1);
      };
      removeFromQueue(opinionQueueByUser);
      removeFromQueue(battleQueueByUser);

      // 인덱스 재빌드(간단히 전체 재계산)
      msgIndexByTurn.clear();
      messages.value.forEach(
        (msg, i) => msg.turnKey && msgIndexByTurn.set(msg.turnKey, i)
      );
    }
  }
});

stt.onOpinionSummary((payload: any) => {
  console.log("onOpinionSummary", payload);
  const user = payload?.user ?? "-";
  const summary = payload?.summary ?? payload?.text ?? JSON.stringify(payload);
  attachSummaryByQueue(user, summary, "normal");
});

stt.onBattleSummary((payload: any) => {
  console.log("onBattleSummary", payload);
  const user = payload?.user ?? "-";
  const summary = payload?.summary ?? payload?.text ?? JSON.stringify(payload);
  attachSummaryByQueue(user, summary, "battle");
});

stt.onResultSummary((payload: any) => {
  console.log("onResultSummary", payload);
  const text = payload?.summary ?? payload?.text ?? JSON.stringify(payload);
  messages.value.push({
    id: messageIdCounter++,
    type: "result",
    timestamp: new Date(),
    summaryText: text,
  });
});

const isRec = computed(() => !!stt.isRecognizing.value);

const firstTeam = ref<Team[]>([]);
const secondTeam = ref<Team[]>([]);

// WebRTC
const {
  // 상태
  state,
  connectionError,
  roomId,
  allParticipantsConnected,
  connectionProgress,
  stepDone,

  // 사용자 정보
  getCurrentUser,
  config,

  // 연결 관리
  startWebRTCConnection,
  disconnectWebRTC,
  replaceLocalAudioTrack,
} = useWebRTCConnection({
  audioController: audioControls,
  participantsCount: roomStore.room?.participants.length ?? 1,
  stompClient: createStompClientWrapper(client.value),
  userInfo: {
    email: currentUserEmail,
    nickname: currentUserSide,
    isLoggedIn: true,
  },
});

const connectionStep = computed(() => {
  return state.value.connectionStep;
});

// 토론 주제
const debateSubject = ref(
  roomStore.room?.topic || "이곳에 토론 주제가 들어갑니다."
);
const debateLeftTeam = ref(roomStore.room?.leftTeamName || "좌측 진영");
const debateRightTeam = ref(roomStore.room?.rightTeamName || "우측 진영");
const debateStartAt = ref("");
const currentTime = ref(0);

// UI 상태 관리
const isDebateInfoCollapsed = ref(false);
const isAnimating = ref(false);

// 토론 단계 관련
const stages = ["진영논리", "1차 요약", "공방전", "투표", "토론결과"];
const currentStageIndex = ref(0);
const currentStage = ref(stages[currentStageIndex.value]);

// 발언 순서
const currentSpeaker = ref("");
const nextSpeaker = ref("");
const speakerStartAt = ref("");
const speakingTimeLeft = ref(0);
const isSpeakingStarted = ref(false);
const speakingTimer = ref<number | null>(null);
const speakingDuration = ref(60 * 1000);
const isLeftSpeaking = ref(false);
const isRightSpeaking = ref(false);

const speakingOrderMap = computed(() => {
  const map: Record<string, number> = {};
  const maxLength = Math.max(
    roomStore.leftTeam.length,
    roomStore.rightTeam.length
  );
  let order = 1;

  for (let i = 0; i < maxLength; i++) {
    // 왼쪽 팀 먼저
    if (i < roomStore.leftTeam.length) {
      const left = roomStore.leftTeam[i];
      map[left.userId] = order;
      order++;
    }

    // 오른쪽 팀 다음
    if (i < roomStore.rightTeam.length) {
      const right = roomStore.rightTeam[i];
      map[right.userId] = order;
      order++;
    }
  }

  return map;
});

const startSpeakingTimer = () => {
  console.log("startSpeakingTimer");
  const serverStartMs = new Date(speakerStartAt.value).getTime();
  currentTime.value = Date.now();
  const elapsedMs = currentTime.value - serverStartMs;
  const remainingMs = Math.max(speakingDuration.value - elapsedMs, 0);
  speakingTimeLeft.value = Math.ceil(remainingMs / 1000);
  isSpeakingStarted.value = true;
  if (speakingTimer.value) {
    clearInterval(speakingTimer.value);
  }
  speakingTimer.value = setInterval(() => {
    speakingTimeLeft.value--;
    if (speakingTimeLeft.value < 0) {
      speakingTimeLeft.value = 0;
      if (speakingTimer.value) {
        clearInterval(speakingTimer.value);
      }
      speakingTimer.value = null;
      isSpeakingStarted.value = false;
    }
  }, 1000);
};

// 공방전 관련 변수
const isSelectTarget = ref(false);
const selectTargetStartAt = ref("");
const SELECT_TARGET_DURATION = 30 * 1000; // 30초
const selectTargetTimeLeft = ref(0);
const selectTargetTimeLeftTimer = ref<number | null>(null);
const selectedPrevTarget = ref<{ userId: string; displayName: string } | null>(
  null
);
const selectedCurrentTarget = ref<{
  userId: string;
  displayName: string;
} | null>(null);
const selectTargetConfirmed = ref(false);
const isAttackPhase = ref(false);
const isDefensePhase = ref(false);

const handleSelectTargetModalClose = (newValue: boolean) => {
  if (selectTargetTimeLeft.value > 0) {
    console.log("모달 닫기 시도가 차단됨 - 아직 제한시간이 남아있습니다.");
    return;
  }

  isSelectTarget.value = newValue;
};

const handleVoteModalClose = (newValue: boolean) => {
  if (voteTimeLeft.value > 0) {
    console.log("모달 닫기 시도가 차단됨 - 아직 제한시간이 남아있습니다.");
    return;
  }

  isVoteTime.value = newValue;
};

const isLocked = computed(() => {
  return isSelectTarget.value && (selectTargetTimeLeft.value ?? 0) > 0;
});

const onUpdateOpen = (newValue: boolean) => {
  console.log("Modal update open:", newValue, "isLocked:", isLocked.value);

  // 락이 걸려있지 않을 때만 모달을 닫을 수 있음
  if (!isLocked.value) {
    isSelectTarget.value = newValue;
  } else if (newValue === false) {
    // 락이 걸려있는 상태에서 닫으려고 하면 무시
    console.log("모달 닫기 시도가 차단됨 - 아직 제한시간이 남아있습니다.");
  }
};

const selectTarget = () => {
  console.log("selectTarget");
  if (!selectedCurrentTarget.value) {
    console.log("선택한 대상이 없습니다.");
    return;
  }

  if (
    selectedPrevTarget.value?.userId === selectedCurrentTarget.value?.userId
  ) {
    console.log("이미 선택한 대상입니다.");
    return;
  }

  selectedPrevTarget.value = selectedCurrentTarget.value;

  console.log("roomStore.room?.roomId", roomStore.room?.roomId);
  console.log("selectedCurrentTarget", selectedCurrentTarget.value);

  // 공격 대상 선택 서버로 전송
  debateClient.value?.publish({
    destination: `/pub/debate/attack`,
    body: JSON.stringify({
      roomId: roomStore.room?.roomId,
      target: selectedCurrentTarget.value?.userId,
    }),
  });
};

const startSelectTargetTimer = () => {
  console.log("startSelectTargetTimer");
  const serverStartMs = new Date(selectTargetStartAt.value).getTime();
  currentTime.value = Date.now();
  const elapsedMs = currentTime.value - serverStartMs;
  const remainingMs = Math.max(SELECT_TARGET_DURATION - elapsedMs, 0);
  selectTargetTimeLeft.value = Math.ceil(remainingMs / 1000);
  isSelectTarget.value = true;

  if (selectTargetTimeLeftTimer.value) {
    clearInterval(selectTargetTimeLeftTimer.value);
  }

  selectTargetTimeLeftTimer.value = setInterval(() => {
    selectTargetTimeLeft.value--;

    if (selectTargetTimeLeft.value <= 0) {
      selectTargetTimeLeft.value = 0;

      // 타이머 정리
      if (selectTargetTimeLeftTimer.value) {
        clearInterval(selectTargetTimeLeftTimer.value);
        selectTargetTimeLeftTimer.value = null;
      }

      // 시간 종료 시 자동으로 모달 닫기
      setTimeout(() => {
        isSelectTarget.value = false;
        selectedCurrentTarget.value = null;
        selectedPrevTarget.value = null;
        console.log("제한시간 종료로 모달이 자동으로 닫혔습니다.");
      }, 2000);
    }
  }, 1000);
};

const selectTargetParticipant = (participant: {
  userId: string;
  displayName: string;
}) => {
  console.log("selectTargetParticipant", participant);
  selectedCurrentTarget.value = participant;
  selectTarget();
};

const updateSelectedTarget = (msg: DebateSelectTargetResponse) => {
  if (msg.attacker === debateStore.myEmail) {
    selectTargetConfirmed.value = true;
  }
};

// 진영별 참여자 정보
const leftTeam = computed(() => roomStore.leftTeam);
const rightTeam = computed(() => roomStore.rightTeam);

const selectedRows = computed(() => {
  const n = Math.max(leftTeam.value.length, rightTeam.value.length);
  return Array.from({ length: n }, (_, i) => ({
    left: leftTeam.value[i] ?? null,
    right: rightTeam.value[i] ?? null,
  }));
});

const wrapName = (name: string) => {
  return name.length > 8 ? name.slice(0, 8) + "\n" + name.slice(8) : name;
};

const attackersFor = (userId: string) => {
  return targetSelectionStore.attackersFor(userId);
};

// 최종 투표
const voteTeam = ref<number | null>(null);
const isVoteTime = ref(false);
const voteStartAt = ref("");
const VOTE_DURATION = 30 * 1000;
const voteTimeLeft = ref(0);
const voteTimer = ref<number | null>(null);
const isVoteDisabled = ref(false);
const voteResult = ref<number | null>(null);
const voteInfo = ref<Record<string, number>>({});
const isTie = ref(false);
const isLoadingAIMessage = ref(true);

const vote = async (userId: string, side: number) => {
  voteTeam.value = side;
  isVoteDisabled.value = true;
  const token = await getAuthToken();
  console.log("vote send", userId, side);
  const response = await fetch(
    `${import.meta.env.VITE_DEBATE_BASE_URL}/rooms/${roomStore.room?.roomId}/vote`,
    {
      method: "POST",
      mode: "cors",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify({ userEmail: userId, team: side }),
    }
  );
  const data = await response.json();
  console.log("vote response", data);
};

const startVoteTimer = () => {
  console.log("startVoteTimer");
  const serverStartMs = new Date(voteStartAt.value).getTime();
  currentTime.value = Date.now();
  const elapsedMs = currentTime.value - serverStartMs;
  const remainingMs = Math.max(VOTE_DURATION - elapsedMs, 0);
  voteTimeLeft.value = Math.ceil(remainingMs / 1000);
  isVoteTime.value = true;
  if (voteTimer.value) {
    clearInterval(voteTimer.value);
  }
  voteTimer.value = setInterval(() => {
    voteTimeLeft.value--;
    if (voteTimeLeft.value < 0) {
      voteTimeLeft.value = 0;
      if (voteTimer.value) {
        clearInterval(voteTimer.value);
      }
      voteTimer.value = null;
      isVoteTime.value = false;
    }
  }, 1000);
};

// 참가자별 공격/수비 상태 관리 (userId는 문자열이므로 string 키 사용)
const participantStates = ref<Record<string, "attack" | "defense" | null>>({});

// 메시지 관련 변수들
const messages = ref<
  Array<{
    id: number;
    type: string;
    mode?: "normal" | "battle";
    isAttacker?: boolean;
    isDefender?: boolean;
    sender?: string;
    timestamp?: Date;
    team?: number;
    profileImage?: string;
    voteInfo?: Record<string, number>;
    voteResult?: number;
    aiInfo?: Record<string, number>;
    sttText?: string;
    summaryText?: string;
    display?: "stt" | "summary";
    summaryPending?: boolean;
    turnKey?: string;
  }>
>([]);
const messagesContainer = ref<HTMLElement>();
const messagesScrollArea = ref<any>();

// STT (Speech-to-Text) 관련 변수들
const recognition = ref<any>(null);
const isSTTRecognizing = ref(false);
const sttSupported = ref(false);
const currentInterimText = ref("");
const currentFinalText = ref("");
const stopTimer = ref<number | null>(null);
const STT_DURATION = 60 * 1000;

const displayedText = (m: any) =>
  m.display === "summary" && m.summaryText ? m.summaryText : m.sttText;

const toggleDisplay = (m: any) => {
  if (!m.summaryText) return;
  m.display = m.display === "summary" ? "stt" : "summary";
};

// 준비시간 관련 변수
const preparationTimeLeft = ref(30);
const isPreparationTime = ref(true);
const isTimerStarted = ref(false);
const preparationTimer = ref<number | null>(null);
const PREPARATION_DURATION = 30 * 1000;

// 준비시간 타이머 시작
const startPreparationTimer = () => {
  console.log("startPreparationTimer");
  const serverStartMs = new Date(debateStartAt.value).getTime();
  currentTime.value = Date.now();
  const elapsedMs = currentTime.value - serverStartMs;
  const remainingMs = Math.max(PREPARATION_DURATION - elapsedMs, 0);
  preparationTimeLeft.value = Math.ceil(remainingMs / 1000);
  isTimerStarted.value = true;
  if (preparationTimer.value) {
    clearInterval(preparationTimer.value);
  }
  preparationTimer.value = setInterval(() => {
    preparationTimeLeft.value--;
    if (preparationTimeLeft.value < 0) {
      preparationTimeLeft.value = 0;
      if (preparationTimer.value) {
        clearInterval(preparationTimer.value);
      }
      preparationTimer.value = null;
      console.log("준비시간 종료. 토론 시작!");
    }
  }, 1000);
};

// 발언 전환시간 관련 변수
const speakingTransitionTimeLeft = ref(0);
const speakingTransitionTimer = ref<number | null>(null);
const SPEAKING_TRANSITION_DURATION = 4 * 1000;
const speakerEndAt = ref("");
const isTransitionStarted = ref(false);

// 발언 전환시간 타이머 시작
const startSpeakingTransitionTimer = () => {
  console.log("startSpeakingTransitionTimer");
  const serverEndMs = new Date(speakerEndAt.value).getTime();
  currentTime.value = Date.now();
  const elapsedMs = currentTime.value - serverEndMs;
  const remainingMs = Math.max(SPEAKING_TRANSITION_DURATION - elapsedMs, 0);
  speakingTransitionTimeLeft.value = Math.ceil(remainingMs / 1000);
  isTransitionStarted.value = true;
  if (speakingTransitionTimer.value) {
    clearInterval(speakingTransitionTimer.value);
  }
  speakingTransitionTimer.value = setInterval(() => {
    speakingTransitionTimeLeft.value--;
    if (speakingTransitionTimeLeft.value < 0) {
      speakingTransitionTimeLeft.value = 0;
      if (speakingTransitionTimer.value) {
        clearInterval(speakingTransitionTimer.value);
      }
      speakingTransitionTimer.value = null;
      isTransitionStarted.value = false;
    }
  }, 1000);
};

watch(
  () => state.value.participants,
  async (list) => {
    await nextTick();
    list.forEach(({ producerUserEmail }) => {
      const stream = state.value.participants.find(
        (p) => p.producerUserEmail === producerUserEmail
      )?.audioStream;
      if (stream) {
        audioControls.connectParticipantAudio(producerUserEmail, stream);
      }
    });
  },
  { deep: true, immediate: true }
);

watch(
  () => allParticipantsConnected.value,
  () => {
    if (allParticipantsConnected.value) {
      console.log("🔍 모든 참가자가 연결되었습니다.");
    }
  },
  { immediate: true }
);

watch(
  () => isPreparationTime.value,
  () => {
    console.log("🔍 isPreparationTime", isPreparationTime.value);
  },
  { immediate: true }
);

// 시청자 채팅 관련
const audienceMessages = ref<
  Array<{
    id: number;
    text: string;
    nickname: string;
    timestamp: Date;
  }>
>([]);
const audienceChatContainer = ref<HTMLElement>();
const audienceChatScrollArea = ref<any>();
const audienceCount = ref(42);
const newChatMessage = ref("");

// 메시지 ID 카운터
let messageIdCounter = 0;
let audienceMessageIdCounter = 0;
let testMessageIndex = 0;

// 시간 포맷팅
const formatTime = (date: Date) => {
  return date.toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
};

// 시청자 채팅 메시지 전송
const sendChatMessage = () => {
  if (newChatMessage.value.trim()) {
    audienceMessages.value.push({
      id: audienceMessageIdCounter++,
      text: newChatMessage.value,
      nickname: authStore.userNickname || "익명",
      timestamp: new Date(),
    });

    newChatMessage.value = "";
  }
};

// 테스트용 메시지 추가 함수 (개발 중에만 사용)
const addTestAIMessage = () => {
  console.log("addTestAIMessage");
  messages.value.push({
    id: messageIdCounter++,
    type: "ai",
    aiInfo: {
      L: 10,
      R: 40,
    },
    timestamp: new Date(),
  });
};

const addTestVoteMessage = () => {
  console.log("addTestVoteMessage");
  messages.value.push({
    id: messageIdCounter++,
    type: "vote",
    voteInfo: {
      "user01@test.com": 0,
      "user02@test.com": 0,
      "user04@test.com": 1,
    },
    voteResult: 0,
    timestamp: new Date(),
  });
};

let STTCount = 0;

const addTestSTTMessage = () => {
  console.log("addTestSTTMessage");
  const testMessages = {
    text: STTCount + "번째 메시지입니다.",
  };

  const last = messages.value[messages.value.length - 1];

  if (STTCount === 0) {
    messages.value.push({
      id: messageIdCounter++,
      type: "stt",
      mode: "normal",
      timestamp: new Date(),
      sender: leftTeam.value[0].displayName,
      sttText: testMessages.text,
    });
  } else {
    last.sttText = [last.sttText, testMessages.text].filter(Boolean).join(" ");
  }
  STTCount++;
};

const addTestMessageNormal = () => {
  console.log("addTestMessage");
  const testMessages = [
    {
      text: "난 사람은 태어날 때부터 착하다고 봄. 아기들 보면 알잖아, 누가 울면 같이 울고, 웃으면 같이 웃고, 남 도와주려는 본능이 있음. 이게 기본값임. 근데 커가면서 이상한 환경, 개판 사회 분위기, 쓰레기 교육 이런 거 만나면 그 마음이 서서히 가려짐. 그렇게 굳어져서 남 등치는 놈도 생기는 거고. 근데 그게 애초에 악해서 그런 건 아님. 주변이 망이라서 그렇게 변한 거임. 그래서 사람을 바꾸려면 성질 고치라는 소리보다, 좋은 환경이랑 제대로 된 교육부터 깔아야 함. 그러면 가려졌던 선한 본성이 다시 살아나고, 오히려 더 강해짐. 난 이게 인간의 진짜 기본값이라고 생각함. 잘못된 건 사람 자체가 아니라 그 사람을 만든 환경임. 환경만 바꿔주면 원래대로 돌아올 가능성 충분하다고 봄.",
      summaryText:
        "사람은 원래 착한데, 환경이 망하면 변함. 환경만 좋아지면 선한 본성은 다시 살아남.",
      sender: leftTeam.value[0].displayName,
      team: 0,
      mode: "normal",
    },
    {
      text: "난 사람은 원래 이기적이고 욕심 많은 존재라고 봄. 애들만 봐도 장난감 뺏고, 자기 거 먼저 챙기려는 게 본능임. 이게 인간 기본값이라서, 안 가르치면 남 생각 안 하고 자기 이익부터 챙기는 게 당연한 거임. 사회 규칙이랑 법, 교육이 있는 이유도 이 본능을 억누르려고 있는 거고. 좋은 환경이라고 해도, 그 속에서 자기 이익을 챙길 기회가 보이면 사람은 결국 그쪽으로 움직임. 겉으로 착한 척해도 속마음은 자기 손해 안 보려고 계산하고 있음. 그래서 난 인간이 본래 선하다는 건 너무 이상적인 생각이라고 봄. 사람은 본래 자기 중심적이고, 선한 행동은 결국 자기 이익이랑 이미지 관리 때문에 하는 경우가 많음. 진짜 착해 보이는 사람도, 상황만 바뀌면 자기 욕심을 드러내는 게 인간 본성이라고 생각함.",
      summaryText:
        "사람은 본래 이기적이고 욕심 많음. 선한 행동도 결국 자기 이익을 위한 계산에서 나옴.",
      sender: rightTeam.value[0].displayName,
      team: 1,
      mode: "normal",
    },
  ];

  const message = testMessages[testMessageIndex];
  const profileImage =
    message.team === 0 ? debateLeftProfile : debateRightProfile;

  messages.value.push({
    id: messageIdCounter++,
    type: "stt",
    sttText: message.text,
    sender: message.sender,
    timestamp: new Date(),
    team: message.team as number,
    profileImage: profileImage,
    display: "summary",
    summaryText: message.summaryText,
    summaryPending: false,
    mode: message.mode as "normal" | "battle",
  });

  if (testMessageIndex === testMessages.length - 1) {
    testMessageIndex = 0;
  } else {
    testMessageIndex++;
  }
};

const addTestMessageBattle = () => {
  console.log("addTestMessageBattle");
  const testMessages = [
    {
      text: "애들이 장난감 뺏는 건 악해서가 아니라 아직 사회 규칙을 배우지 못해서임. 이기적 행동은 본성이라기보다 미성숙함의 결과고, 성장하면서 배려와 공감 능력이 발달함. 선한 본성이 교육과 경험으로 드러나는 거지, 본래부터 악한 건 아님.",
      summaryText: "이기심은 미성숙함의 결과일 뿐, 본성은 선함.",
      sender: leftTeam.value[1].displayName,
      team: 0,
      mode: "battle",
      isAttacker: true,
    },
    {
      text: "사람이 착한 행동을 배우는 것도 결국 규칙과 처벌, 보상의 영향임. 선한 본성이 있다면 왜 제도와 교육이 없으면 쉽게 무너질까? 착해 보이는 행동도 환경이 억누르기 때문에 가능한 거고, 본성은 여전히 이기적인 상태임.",
      summaryText: "선함은 제도·환경의 억제 결과일 뿐, 본성은 이기적임.",
      sender: rightTeam.value[1].displayName,
      team: 1,
      mode: "battle",
      isDefender: true,
    },
  ];

  const message = testMessages[testMessageIndex];
  const profileImage =
    message.team === 0 ? debateLeftProfile : debateRightProfile;

  messages.value.push({
    id: messageIdCounter++,
    type: "stt",
    sttText: message.text,
    sender: message.sender,
    timestamp: new Date(),
    team: message.team as number,
    profileImage: profileImage,
    display: "summary",
    summaryText: message.summaryText,
    summaryPending: false,
    mode: message.mode as "normal" | "battle",
    isAttacker: message.isAttacker,
    isDefender: message.isDefender,
  });

  if (testMessageIndex === testMessages.length - 1) {
    testMessageIndex = 0;
  } else {
    testMessageIndex++;
  }
};

// 테스트용 시청자 채팅 추가
const addTestAudienceChat = () => {
  console.log("addTestAudienceChat");
  const testChats = [
    { text: "이것은 짧은 채팅", nickname: "짧은닉" },
    {
      text: "나는 보통정도로 긴 채팅을 했어 ㅋㅋㅋㅋㅋㅋ 댓글 길이를 늘리기 위해 아무말이나 말하는 중이야",
      nickname: "보통정도로긴닉네임",
    },
    { text: "zzzzzz", nickname: "영어랑english123" },
    {
      text: "이거는 엄청나게 긴 채팅이 될거야. 아무말이나 그냥 말하면서 길이를 늘려야하지. 코딩을 하다보면 이런저런 일이 생길 수 있어. 예를들면 깃 풀 했는데 충돌이 나서 울면서 충돌을 해결하거나 잘못 덮어씌우는 바람에 그동안 하던 작업이 날아갈 때도 있지. 그러므로 저장을 습관화하고 문제가 생길 것 같을 때는 다른 곳에다 백업을 해야 해.",
      nickname: "onlyenglishnick",
    },
  ];

  const randomChat = testChats[Math.floor(Math.random() * testChats.length)];

  audienceMessages.value.push({
    id: audienceMessageIdCounter++,
    text: randomChat.text,
    nickname: randomChat.nickname,
    timestamp: new Date(),
  });
};

// 토론 정보 영역 토글 함수
const toggleDebateInfo = () => {
  isDebateInfoCollapsed.value = !isDebateInfoCollapsed.value;
};

const debateClient = ref<any>(null);

const connectToDebateRoom = async () => {
  console.log("🔍 connectToDebateRoom");
  const authToken = await getAuthToken();
  return new Promise((resolve, reject) => {
    try {
      // @stomp/stompjs Client 동적 import
      import("@stomp/stompjs")
        .then(({ Client }) => {
          debateClient.value = new Client({
            brokerURL: `${import.meta.env.VITE_DEBATE_WS_URL}/ws`,
            connectHeaders: {
              Authorization: `Bearer ${authToken}`,
              login: currentUserEmail,
            },
            debug: (str) => {
              console.log("내부 STOMP Debug:", str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: (frame) => {
              console.log("✅ 내부 STOMP 연결됨:", frame);
              resolve(debateClient.value);
            },
            onDisconnect: (frame) => {
              console.log("❌ 내부 STOMP 연결 해제됨:", frame);
            },
            onStompError: (frame) => {
              console.error("💥 내부 STOMP 오류:", frame);
              reject(new Error(`STOMP 오류: ${frame.headers.message}`));
            },
            onWebSocketError: (error) => {
              console.error("🔌 내부 WebSocket 오류:", error);
              reject(error);
            },
          });
          debateClient.value.activate();
        })
        .catch(reject);
    } catch (error) {
      reject(error);
    }
  });
};

const subscribeToDebateRoom = (client: Client) => {
  console.log("🔍 subscribeToDebateRoom");

  if (client) {
    client.subscribe(
      `/sub/debate/room/${roomStore.room?.roomId}/start/opinion`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateStartMessage;
        console.log("🔍 토론 시작 알림: ", msg);
        stepDone.completed.resolve();
        debateSubject.value = msg.topicText;
        debateLeftTeam.value = msg.firstOption;
        debateRightTeam.value = msg.secondOption;
        firstTeam.value = msg.firstTeam;
        secondTeam.value = msg.secondTeam;
        debateStartAt.value = msg.debateStartAt + "+09:00";
        startPreparationTimer();
      }
    );
    client.subscribe(
      `/sub/debate/room/${roomStore.room?.roomId}/speak/start`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSpeakStartMessage;
        console.log("🔍 진영논리 발언 시작: ", msg);
        currentSpeaker.value = msg.speaker;
        speakerStartAt.value = msg.speakerStartAt + "+09:00";
        currentStageIndex.value = 0;
        currentStage.value = stages[currentStageIndex.value];
        speakingDuration.value = 60 * 1000;
        if (roomStore.leftTeam.find((p) => p.userId === currentSpeaker.value)) {
          isLeftSpeaking.value = true;
          isRightSpeaking.value = false;
        } else {
          isRightSpeaking.value = true;
          isLeftSpeaking.value = false;
        }
        isPreparationTime.value = false;
        nextSpeaker.value = "";
        stt.startOpinion();
        startSpeakingTimer();
      }
    );
    client.subscribe(
      `/sub/debate/room/${roomStore.room?.roomId}/speak/end`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSpeakEndMessage;
        console.log("🔍 진영논리 발언 종료: ", msg);
        speakerEndAt.value = msg.speakerEndAt + "+09:00";
        if (isLeftSpeaking.value) {
          isLeftSpeaking.value = false;
        }
        if (isRightSpeaking.value) {
          isRightSpeaking.value = false;
        }
        nextSpeaker.value = msg.nextSpeaker;
        stt.stop();
        startSpeakingTransitionTimer();
      }
    );
    client.subscribe(
      `/sub/debate/room/${roomStore.room?.roomId}/start/battle`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSelectTargetMessage;
        console.log("🔍 공격 선택 시작: ", msg);
        isSelectTarget.value = true;
        selectTargetStartAt.value = msg.battleStartAt + "+09:00";
        currentStageIndex.value = 2;
        startSelectTargetTimer();
      }
    );
    client.subscribe(
      `/sub/debate/room/${roomStore.room?.roomId}/attack`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSelectTargetResponse;
        console.log("🔍 공격 선택 응답: ", msg);
        if (msg.attacker === debateStore.myEmail) {
          selectTargetConfirmed.value = true;
        }
        targetSelectionStore.applySelect(msg);
      }
    );
    client.subscribe(
      `/sub/debate/room/${roomStore.room?.roomId}/speak/attackStart`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSpeakStartMessage;
        console.log("🔍 공격 발언 시작: ", msg);
        isSelectTarget.value = false;
        currentSpeaker.value = msg.speaker;
        speakerStartAt.value = msg.speakerStartAt + "+09:00";
        currentStageIndex.value = 2;
        currentStage.value = stages[currentStageIndex.value];
        speakingDuration.value = 30 * 1000;
        isAttackPhase.value = true;
        // 공격자 상태 설정
        participantStates.value[msg.speaker] = "attack";
        if (roomStore.leftTeam.find((p) => p.userId === currentSpeaker.value)) {
          isLeftSpeaking.value = true;
          isRightSpeaking.value = false;
        } else {
          isRightSpeaking.value = true;
          isLeftSpeaking.value = false;
        }
        nextSpeaker.value = "";
        stt.startBattleAttack();
        startSpeakingTimer();
      }
    );
    client.subscribe(
      `/sub/debate/room/${roomStore.room?.roomId}/speak/attackEnd`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSpeakEndMessage;
        console.log("🔍 공격 발언 종료: ", msg);
        speakerEndAt.value = msg.speakerEndAt + "+09:00";
        // 공격자 상태 해제
        if (currentSpeaker.value) {
          participantStates.value[currentSpeaker.value] = null;
        }
        if (isLeftSpeaking.value) {
          isLeftSpeaking.value = false;
        }
        if (isRightSpeaking.value) {
          isRightSpeaking.value = false;
        }
        nextSpeaker.value = msg.nextSpeaker;
        stt.stop();
        startSpeakingTransitionTimer();
      }
    );
    client.subscribe(
      `/sub/debate/room/${roomStore.room?.roomId}/speak/defenseStart`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSpeakStartMessage;
        console.log("🔍 방어 발언 시작: ", msg);
        currentSpeaker.value = msg.speaker;
        speakerStartAt.value = msg.speakerStartAt + "+09:00";
        currentStageIndex.value = 2;
        currentStage.value = stages[currentStageIndex.value];
        speakingDuration.value = 30 * 1000;
        isDefensePhase.value = true;
        // 방어자 상태 설정
        participantStates.value[msg.speaker] = "defense";
        if (roomStore.leftTeam.find((p) => p.userId === currentSpeaker.value)) {
          isLeftSpeaking.value = true;
        } else {
          isRightSpeaking.value = true;
        }
        nextSpeaker.value = "";
        stt.startBattleDefense();
        startSpeakingTimer();
      }
    );
    client.subscribe(
      `/sub/debate/room/${roomStore.room?.roomId}/speak/defenseEnd`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSpeakEndMessage;
        console.log("🔍 방어 발언 종료: ", msg);
        speakerEndAt.value = msg.speakerEndAt + "+09:00";
        // 방어자 상태 해제
        if (currentSpeaker.value) {
          participantStates.value[currentSpeaker.value] = null;
        }
        if (isLeftSpeaking.value) {
          isLeftSpeaking.value = false;
        }
        if (isRightSpeaking.value) {
          isRightSpeaking.value = false;
        }
        nextSpeaker.value = msg.nextSpeaker;
        stt.stop();
        startSpeakingTransitionTimer();
      }
    );
    client.subscribe(
      `/sub/debate/room/${roomStore.room?.roomId}/vote/start`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateVoteStartMessage;
        console.log("🔍 투표 시작 알림: ", msg);
        isVoteTime.value = true;
        voteStartAt.value = msg.voteStartAt + "+09:00";
        voteTimeLeft.value = Math.ceil(VOTE_DURATION / 1000);
        currentStageIndex.value = 3;
        currentStage.value = stages[currentStageIndex.value];
        nextSpeaker.value = "";
        startVoteTimer();
      }
    );
    client.subscribe(
      `/sub/debate/room/${roomStore.room?.roomId}/vote/end`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateVoteEndMessage;
        console.log("🔍 투표 종료 알림: ", msg);
        isVoteTime.value = false;
        voteTimeLeft.value = 0;
        currentStageIndex.value = 4;
        currentStage.value = stages[currentStageIndex.value];
        voteTeam.value = null;
        isVoteDisabled.value = false;
        voteInfo.value = msg.voteInfo;
        voteResult.value = msg.voteResult;
        if (msg.voteResult === 2) {
          isTie.value = true;
        }
        messages.value.push({
          id: messageIdCounter++,
          type: "vote",
          voteResult: msg.voteResult,
          voteInfo: msg.voteInfo,
          timestamp: new Date(),
        });
      }
    );
  }
};

// 컴포넌트 마운트 시 초기화
onMounted(async () => {
  console.log("🚀 토론방 초기화 시작");

  // debateStore.setRoomId("11");
  // debateStore.setMyInfo("user01@test.com", "L", false);

  setTimeout(() => {
    // stepDone.completed.resolve();
    // state.value.isConnecting = false;
    // startPreparationTimer();
    // isPreparationTime.value = false;
    // isTransitionStarted.value = true;
    // startSpeakingTransitionTimer();
    // startSpeakingTimer();
    // isSelectTarget.value = true;
    // startSelectTargetTimer();
    // isVoteTime.value = true;
    // startVoteTimer();
    // addTestMessageNormal();
    // addTestMessageNormal();
    // addTestMessageBattle();
    // addTestMessageBattle();
    // addTestVoteMessage();
    // addTestAIMessage();
  }, 1000);

  await connectToDebateRoom();
  subscribeToDebateRoom(debateClient.value!);
  await startWebRTCConnection(roomStore.room?.roomId ?? "");
  console.log("🔍 토론방 준비 완료: ", Date.now());
  debateClient.value.publish({
    destination: `/pub/debate/room/${roomStore.room?.roomId}/join`,
    body: JSON.stringify({
      roomId: roomStore.room?.roomId,
    }),
  });
});

// 컴포넌트 언마운트 시 STOMP 연결 해제 및 정리
onUnmounted(() => {
  // WebRTC 연결 해제
  disconnectWebRTC();

  // 토론 상태 초기화 (방을 나갈 때는 resetAll 사용)
  debateStore.resetDebateState();

  // STOMP 연결 해제 및 정리
  if (debateClient.value) {
    debateClient.value.deactivate();
    debateClient.value = null;
  }
});
</script>

<style scoped>
/* 발언 중인 참가자의 프로필 이미지 초록색 빛남 효과 */
:deep(.speaking-glow) {
  position: relative;
}

/* 남은 시간 깜빡임 */
.blink {
  animation: blink 1s steps(2, start) infinite;
}

@keyframes blink {
  to {
    visibility: hidden;
  }
}

:deep(.speaking-glow)::after {
  content: "";
  position: absolute;
  top: 0px;
  left: 0px;
  right: 0px;
  bottom: 0px;
  border-radius: 50%;
  box-shadow:
    0 0 12px 3px rgba(16, 185, 129, 0.8),
    0 0 24px 6px rgba(16, 185, 129, 0.45);
  animation: green-breathe 2.2s ease-in-out infinite;
}

@keyframes green-breathe {
  0%,
  100% {
    box-shadow:
      0 0 12px 3px rgba(16, 185, 129, 0.8),
      0 0 24px 6px rgba(16, 185, 129, 0.45);
  }
  50% {
    box-shadow:
      0 0 18px 4px rgba(16, 185, 129, 0.95),
      0 0 36px 10px rgba(16, 185, 129, 0.55);
  }
}
</style>
