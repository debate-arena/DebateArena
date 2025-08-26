<template>
  <div
    class="bg-gradient-to-br relative"
    :class="[isPreparationTime ? 'min-h-[max(800px,calc(100vh-64px))]' : '']"
  >
    <!-- 시작 전 화면 -->
    <div v-if="isPreparationTime">
      <!-- WebRTC 연결 중 화면 -->
      <div
        v-if="state.isConnecting && !isTimerStarted"
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
                    <template v-else> 대기시간 </template>
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
                        width: `${((speakingTransitionTimeLeft || 0) / (SPEAKING_TRANSITION_DURATION / 1000)) * 100}%`,
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
                              (
                                participant.userId === debateStore.myEmail
                                  ? audioControls.getLocalSpeaking()
                                  : audioControls.getParticipantSpeaking(
                                      participant.userId
                                    )
                              )
                                ? 'speaking-glow'
                                : '',
                            ]"
                          >
                            <AvatarImage
                              :src="debateLeftProfile"
                              :alt="participant.displayName"
                              class="border-2 border-black rounded-full bg-white"
                            />
                            <!-- 발언자 아이콘 -->
                            <div
                              v-if="participant.userId === currentSpeaker"
                              class="absolute top-6 z-10 w-16 h-16 flex items-center justify-center text-2xl"
                            >
                              🗣️
                            </div>
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
                              (
                                participant.userId === debateStore.myEmail
                                  ? audioControls.getLocalSpeaking()
                                  : audioControls.getParticipantSpeaking(
                                      participant.userId
                                    )
                              )
                                ? 'speaking-glow'
                                : '',
                            ]"
                          >
                            <AvatarImage
                              :src="debateRightProfile"
                              :alt="participant.displayName"
                              class="border-2 border-black rounded-full bg-white"
                            />
                            <!-- 발언자 아이콘 -->
                            <div
                              v-if="participant.userId === currentSpeaker"
                              class="absolute top-6 z-10 w-16 h-16 flex items-center justify-center text-2xl"
                            >
                              🗣️
                            </div>
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
                        :alt="senderNameOf(message)"
                        class="border-2 border-black rounded-full bg-white"
                      />
                      <div
                        v-if="message.isAttacker || message.isDefender"
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
                    <span v-if="senderNameOf(message).length <= 8">
                      {{ senderNameOf(message) }}
                    </span>
                    <span v-else class="block">
                      {{ senderNameOf(message).slice(0, 8) }}<br />{{ senderNameOf(message).slice(8) }}
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
                        ? 'bg-[hsl(var(--debate-left-deep))] text-white ml-28 mr-28'
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
                        class="px-2 py-0.5 rounded-full bg-white text-black"
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
                      'relative p-8 rounded-4xl text-lg shadow-sm mx-28 bg-[hsl(var(--debate-random-bg))]',
                    ]"
                    style="
                      min-height: 80px;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                    "
                  >
                    <div class="flex flex-col items-center text-black">
                      <h2 class="text-3xl font-bold">투표 결과</h2>
                      <div
                        class="flex flex-row text-center justify-center gap-8 mt-4"
                      >
                        <div class="flex flex-col items-center">
                          <div
                            class="text-4xl font-bold text-[hsl(var(--debate-left-deep))]"
                          >
                            {{
                              Object.values(message.voteInfo ?? {}).filter(
                                (v) => v === 0
                              ).length
                            }}
                          </div>
                        </div>
                        <div class="text-3xl font-bold self-center">VS</div>
                        <div class="flex flex-col items-center">
                          <div
                            class="text-4xl font-bold text-[hsl(var(--debate-right-deep))]"
                          >
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
                            class="w-12 h-12 rounded-full mt-2 flex items-center justify-center shadow-md font-bold border-2 border-black text-white"
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
                            class="w-12 h-12 rounded-full mt-2 flex items-center justify-center shadow-md font-bold border-2 border-black text-white"
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

<!-- AI 최종 판정 결과 -->
<div
  v-else-if="message.resultType === 'ai_judgment'"
  :class="[
    'relative p-8 rounded-4xl text-lg shadow-sm mx-28 bg-[hsl(var(--debate-random-bg))] text-black',
  ]"
>
  <div class="flex flex-col items-center">
    <h2 class="text-3xl font-bold mb-6">🤖 AI 최종 판정</h2>

    <!-- 승리팀 아이콘/문구 (바로 아래 표시) -->
    <div class="mb-6">
      <template v-if="message.aiWinner === 'num1'">
        <div class="flex items-center justify-center">
          <img :src="debateLeftProfile" alt="북극곰" class="w-20 h-20 mr-4" />
          <h3 class="text-3xl font-bold text-[hsl(var(--debate-left-deep))]">
            🎉 좌측(북극곰) 승리!
          </h3>
        </div>
      </template>
      <template v-else-if="message.aiWinner === 'num2'">
        <div class="flex items-center justify-center">
          <img :src="debateRightProfile" alt="펭귄" class="w-20 h-20 mr-4" />
          <h3 class="text-3xl font-bold text-[hsl(var(--debate-right-deep))]">
            🎉 우측(펭귄) 승리!
          </h3>
        </div>
      </template>
      <template v-else>
        <h3 class="text-2xl font-bold">무승부</h3>
      </template>
    </div>

    <!-- 🧠 AI 청중단 판정 결과 (집계표 = result.votes) -->
    <div v-if="message.aiVotes" class="mb-6">
      <h4 class="text-xl font-bold mb-2 text-center">🧠 AI 청중단 판정 결과</h4>
      <div class="flex items-center justify-center space-x-8">
        <div class="text-center">
          <div class="text-3xl font-bold text-[hsl(var(--debate-left-deep))]">
            {{ message.aiVotes.num1 || 0 }}
          </div>
          <p class="text-gray-600">좌측팀</p>
        </div>
        <div class="text-2xl font-bold text-gray-500">VS</div>
        <div class="text-center">
          <div class="text-3xl font-bold text-[hsl(var(--debate-right-deep))]">
            {{ message.aiVotes.num2 || 0 }}
          </div>
          <p class="text-gray-600">우측팀</p>
        </div>
      </div>
    </div>

    <!-- 📝 판정 근거 -->
    <div v-if="message.judgmentReason" class="mb-6 bg-white/20 rounded-2xl p-6 w-full">
      <h4 class="text-xl font-bold mb-3 text-center">📝 판정 근거</h4>
      <p class="text-gray-700 leading-relaxed text-center">
        {{ message.judgmentReason }}
      </p>
    </div>

    <!-- 팀 요약: 승리팀 → 패배팀 -->
    <div class="w-full grid grid-cols-1 gap-8">
      <!-- 승리팀 요약 먼저 -->
      <template v-if="message.aiWinner === 'num1'">
        <div class="bg-white/20 rounded-2xl p-6">
          <div class="flex items-center justify-center mb-4">
            <img :src="debateLeftProfile" alt="북극곰" class="w-16 h-16 mr-3" />
            <h3 class="text-2xl font-bold text-[hsl(var(--debate-left-deep))]">
              승리팀 요약(좌측)
            </h3>
          </div>
          <p class="text-gray-700 leading-relaxed text-center">
            {{ message.num1Summary || '요약 정보 없음' }}
          </p>
        </div>
        <div class="bg-white/20 rounded-2xl p-6">
          <div class="flex items-center justify-center mb-4">
            <img :src="debateRightProfile" alt="펭귄" class="w-16 h-16 mr-3" />
            <h3 class="text-2xl font-bold text-[hsl(var(--debate-right-deep))]">
              패배팀 요약(우측)
            </h3>
          </div>
          <p class="text-gray-700 leading-relaxed text-center">
            {{ message.num2Summary || '요약 정보 없음' }}
          </p>
        </div>
      </template>

      <template v-else-if="message.aiWinner === 'num2'">
        <div class="bg-white/20 rounded-2xl p-6">
          <div class="flex items-center justify-center mb-4">
            <img :src="debateRightProfile" alt="펭귄" class="w-16 h-16 mr-3" />
            <h3 class="text-2xl font-bold text-[hsl(var(--debate-right-deep))]">
              승리팀 요약(우측)
            </h3>
          </div>
          <p class="text-gray-700 leading-relaxed text-center">
            {{ message.num2Summary || '요약 정보 없음' }}
          </p>
        </div>
        <div class="bg-white/20 rounded-2xl p-6">
          <div class="flex items-center justify-center mb-4">
            <img :src="debateLeftProfile" alt="북극곰" class="w-16 h-16 mr-3" />
            <h3 class="text-2xl font-bold text-[hsl(var(--debate-left-deep))]">
              패배팀 요약(좌측)
            </h3>
          </div>
        <p class="text-gray-700 leading-relaxed text-center">
            {{ message.num1Summary || '요약 정보 없음' }}
          </p>
        </div>
      </template>

      <!-- 승패 표시가 불명확/무승부일 때는 양쪽 모두 순서 고정 -->
      <template v-else>
        <div class="bg-white/20 rounded-2xl p-6">
          <div class="flex items-center justify-center mb-4">
            <img :src="debateLeftProfile" alt="북극곰" class="w-16 h-16 mr-3" />
            <h3 class="text-2xl font-bold text-[hsl(var(--debate-left-deep))]">좌측 요약</h3>
          </div>
          <p class="text-gray-700 leading-relaxed text-center">
            {{ message.num1Summary || '요약 정보 없음' }}
          </p>
        </div>
        <div class="bg-white/20 rounded-2xl p-6">
          <div class="flex items-center justify-center mb-4">
            <img :src="debateRightProfile" alt="펭귄" class="w-16 h-16 mr-3" />
            <h3 class="text-2xl font-bold text-[hsl(var(--debate-right-deep))]">우측 요약</h3>
          </div>
          <p class="text-gray-700 leading-relaxed text-center">
            {{ message.num2Summary || '요약 정보 없음' }}
          </p>
        </div>
      </template>
    </div>
  </div>
</div>

                <!-- AI 판정단 결과 -->
                <div v-if="message.type === 'ai' && isTie">
                  <div
                    :class="[
                      'relative p-8 rounded-4xl text-lg shadow-sm mx-28 bg-[hsl(var(--debate-random-bg))] text-black',
                    ]"
                  >
                    <div class="flex flex-col items-center">
                      <h2 class="text-3xl font-bold">AI 판정단 결과</h2>
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
                          <div
                            class="text-6xl font-bold text-[hsl(var(--debate-left-deep))]"
                          >
                            {{ message.aiInfo?.L }}
                          </div>
                          <div
                            class="text-6xl font-bold text-[hsl(var(--debate-right-deep))]"
                          >
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
        <DialogContent class="z-[60] w-[1000px] max-w-none">
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
                    class="text-xs font-semibold text-center leading-tight h-[2.6em] flex items-center justify-center"
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
                <div class="font-bold flex-1 min-w-0 break-words p-4 text-left">
                  {{ lastTextFor(participant.userId) }}
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
                    class="text-xs font-semibold text-center leading-tight h-[2.6em] flex items-center justify-center"
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
                <div class="font-bold flex-1 min-w-0 break-words p-4 text-left">
                  {{ lastTextFor(participant.userId) }}
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
                    <Avatar :class="['w-16 h-16 overflow-visible']">
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
                    <Avatar :class="['w-16 h-16 overflow-visible']">
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
        <DialogContent class="z-[60] w-[1000px] max-w-none">
          <DialogHeader>
            <DialogTitle class="text-center text-3xl">최종 투표</DialogTitle>
            <DialogTitle class="text-center text-2xl py-2">
              {{ debateSubject }}
            </DialogTitle>
          </DialogHeader>
          <!-- 발화자가 아닌 경우 최종 투표중 메시지 표시 -->
          <div v-if="!roomStore.leftTeam.some(p => p.userId === debateStore.myEmail) && !roomStore.rightTeam.some(p => p.userId === debateStore.myEmail)" class="text-center py-8">
            <div class="text-2xl font-bold text-gray-600 mb-4">최종 투표중</div>
            <div class="text-lg text-gray-500">참가자들이 투표를 진행하고 있습니다.</div>
          </div>
          <!-- 참가자인 경우 투표 버튼 표시 -->
          <div v-else class="grid grid-cols-2 gap-4 text-center">
            <Button
              variant="outline"
              class="w-full h-full hover:bg-[hsl(var(--debate-left-deep))] rounded-xl bg-[hsl(var(--debate-left-deep))] text-white hover:-translate-y-0.5 transition-all duration-300 disabled:opacity-70"
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
                  <div
                    class="text-2xl whitespace-pre-wrap text-center leading-tight"
                  >
                    {{
                      debateLeftTeam && debateLeftTeam.length > 10
                        ? debateLeftTeam.substring(0, 10) +
                          "\n" +
                          debateLeftTeam.substring(10)
                        : debateLeftTeam
                    }}
                  </div>
                </CardContent>
              </Card>
            </Button>
            <Button
              variant="outline"
              class="w-full h-full rounded-xl hover:bg-[hsl(var(--debate-right-deep))] bg-[hsl(var(--debate-right-deep))] text-white hover:-translate-y-0.5 transition-all duration-300 disabled:opacity-70"
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
                  <div
                    class="text-2xl whitespace-pre-wrap text-center leading-tight"
                  >
                    {{
                      debateRightTeam && debateRightTeam.length > 10
                        ? debateRightTeam.substring(0, 10) +
                          "\n" +
                          debateRightTeam.substring(10)
                        : debateRightTeam
                    }}
                  </div>
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
                        :src="chatMessage.team === 0 ? debateLeftProfile : chatMessage.team === 1 ? debateRightProfile : audienceProfile"
                        :alt="chatMessage.nickname"
                        class="border-2 border-black rounded-full bg-white"
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
  DebateStartMessage,
  DebateSpeakStartMessage,
  DebateSpeakEndMessage,
  DebateSelectTargetMessage,
  DebateSelectTargetResponse,
  DebateVoteStartMessage,
  DebateVoteEndMessage,
  DebateChatMessage,
} from "@/types/debate";
import {
  useWebRTCConnection,
  type StompClient,
} from "@/composables/useWebRTCConnection";
import { useAudioControls } from "@/composables/useAudioControls";
import { useRoomStore } from "@/store/roomStore";
import { 
  isVoteDecided, 
  isAIJudgment, 
  extractVoteDecidedData, 
  extractAIJudgmentData,
  analyzeAIResponse 
} from '@/utils/debateResult';
import { Client } from "@stomp/stompjs";
import {
  Dialog,
  DialogContent,
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

const withKstIfNaive = (s?: string | null) => {
  if (!s) return s;
  if (/[Zz]$/.test(s) || /[+-]\d{2}:\d{2}$/.test(s)) return s;
  return s + "+09:00";
};

const parseMs = (s?: string | null, defaultZone = "+09:00") => {
  if (!s) return NaN;
  let str = s.trim().replace(" ", "T");
  const zoneMatch = str.match(/(Z|[+\-]\d{2}:?\d{2})$/i);
  let zone = "";
  if (zoneMatch) {
    zone = zoneMatch[1] === "Z" ? "Z" : zoneMatch[1].replace(/([+\-]\d{2})(\d{2})$/, "$1:$2");
    str = str.slice(0, -zoneMatch[1].length);
  } else {
    zone = defaultZone;
  }
  str = str.replace(/(\d{2}:\d{2}:\d{2})(\.\d+)?$/, (_, hms: string, frac?: string) => {
    if (!frac) return `${hms}`;
    const ms3 = frac.slice(1, 4).padEnd(3, "0");
    return `${hms}.${ms3}`;
  });
  const iso = `${str}${zone}`;
  const t = Date.parse(iso);
  return Number.isFinite(t) ? t : NaN;
};

const senderNameOf = (m: any) => m?.senderDisplayName || m?.sender || '';

const lastTextFor = (userId: string) => {
  // 최신 STT 메시지 한 건 찾기
  const last = [...messages.value]
    .reverse()
    .find(m => m.type === 'stt' && m.sender === userId);

  if (!last) return '메시지 없음';
  if (last.summaryPending) return '요약중…';

  // 말풍선의 표시 상태를 따라가고, 요약이 없으면 원문으로 폴백
  return (last.display === 'summary' && last.summaryText)
    ? last.summaryText
    : (last.sttText || '메시지 없음');
};


const juryVotesOf = (payloadOrVotes: any) => {
  const v =
    (payloadOrVotes?.result && payloadOrVotes.result.votes) ??
    payloadOrVotes?.votes ??
    null;
  if (!v) return null;
  return {
    num1: Number(v.num1 ?? 0),
    num2: Number(v.num2 ?? 0),
    // 필요하면 none 표시도 가능: none: Number(v.none ?? 0)
  };
};
// Stores
const debateStore = useDebateStore();
const roomStore = useRoomStore();
const authStore = useAuthStore();
const targetSelectionStore = useTargetSelectionStore();
const audioControls = useAudioControls();

const roomIdParams = route.params.id as string;

const currentUserEmail = authStore.userEmail || '';
const myParticipant = computed(() => 
  roomStore.room?.participants.find(p => p.userId === currentUserEmail)
);
const currentUserSide = computed(() => myParticipant.value?.side || "L");
const isModerator = false;
const isViewerParam = route.query.isViewer as string | undefined;
const isViewer = isViewerParam === "true";

debateStore.setRoomId(roomIdParams);
debateStore.setMyInfo(
  currentUserEmail,
  (isViewer ? null : (currentUserSide.value as "L" | "R")),
  isModerator
);

// @stomp/stompjs Client를 StompClient 인터페이스로 래핑
const createStompClientWrapper = (client: any): StompClient | undefined => {
  if (!client) return undefined;
  return {
    connected: client.connected,
    connect: () => {},
    disconnect: () => client.deactivate(),
    subscribe: (destination: string, callback: (message: any) => void) => {
      const subscription = client.subscribe(destination, callback);
      return { unsubscribe: () => subscription.unsubscribe() };
    },
    publish: (destination: string, body: any) => {
      client.publish({
        destination,
        body: typeof body === "string" ? body : JSON.stringify(body),
      });
    },
  };
};

// STT
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
  const turnKey = q.shift();
  queueMap.set(user, q);
  if (!turnKey) return;
  const idx = msgIndexByTurn.get(turnKey);
  if (idx == null) return;
  const m = messages.value[idx];
  m.summaryText = summary;
  m.summaryPending = false;
  m.display = "summary";
}

const makeTurnKey = (user: string, startedAt: string) => `${user}|${startedAt}`;

const stompRef = ref<any>(null);

const stt = useStt(
  stompRef,
  computed(() => String(roomStore.room?.roomId ?? '')),
  { lang: 'ko-KR' }
);

stt.onSttMessage((payload: any) => {
  const body = payload?.data?.text ? payload.data : payload;
  const userId = body?.user ?? "-";
  const text = body?.text ?? JSON.stringify(payload);
  const now = new Date();
    // 참가자 정보에서 표시이름 조회
  const p = roomStore.room?.participants.find(p => p.userId === userId);
  const displayName = p?.displayName ?? userId;

  const turnKey = makeTurnKey(userId, speakerStartAt.value);

  const messageMode: "normal" | "battle" | undefined =
    currentStageIndex.value < 2
      ? "normal"
      : currentStageIndex.value < 4
        ? "battle"
        : undefined;

  let idx = msgIndexByTurn.get(turnKey);

  if (idx == null) {
    const side = p?.side;
    const team = side === "L" ? 0 : side === "R" ? 1 : undefined;

    messages.value.push({
      id: messageIdCounter++,
      type: "stt",
      mode: messageMode,
      team,
      timestamp: now,
      sender: userId,
      senderDisplayName: displayName,
      sttText: "",
      isAttacker:
        currentStageIndex.value >= 2 &&
        currentSpeaker.value === userId &&
        isAttackPhase.value
          ? true
          : undefined,
      isDefender:
        currentStageIndex.value >= 2 &&
        currentSpeaker.value === userId &&
        isDefensePhase.value
          ? true
          : undefined,
      summaryPending: true,
      display: "stt",
      turnKey,
    });
    idx = messages.value.length - 1;
    msgIndexByTurn.set(turnKey, idx);
    const q = messageMode === "battle" ? (battleQueueByUser.get(userId) ?? []) : (opinionQueueByUser.get(userId) ?? []);
    q.push(turnKey);
    messageMode === "battle" ? battleQueueByUser.set(userId, q) : opinionQueueByUser.set(userId, q);
  }

  const m = messages.value[idx];
  m.sttText = [m.sttText, text].filter(Boolean).join(" ");
  m.timestamp = now;

  const LIMIT = 200;
  if (messages.value.length > LIMIT) {
    const removed = messages.value.shift();
    if (removed?.turnKey) {
      msgIndexByTurn.delete(removed.turnKey);
      const removeFromQueue = (q: Map<string, string[]>) => {
        const arr = q.get(removed.sender ?? "");
        if (!arr) return;
        const i = arr.indexOf(removed.turnKey!);
        if (i >= 0) arr.splice(i, 1);
      };
      removeFromQueue(opinionQueueByUser);
      removeFromQueue(battleQueueByUser);
      msgIndexByTurn.clear();
      messages.value.forEach(
        (msg, i) => msg.turnKey && msgIndexByTurn.set(msg.turnKey, i)
      );
    }
  }
});

stt.onOpinionSummary((payload: any) => {
  const result = payload?.result || payload;
  const user = result?.user_id ?? result?.user ?? payload?.user ?? "-";
  const summary = result?.text ?? payload?.summary ?? payload?.text ?? JSON.stringify(payload);
  attachSummaryByQueue(user, summary, "normal");
});

stt.onBattleSummary((payload: any) => {
  const result = payload?.result || payload;
  const attack_id = result?.attack_id ?? result?.attacker ?? payload?.attack_id ?? "-";
  const defense_id = result?.defense_id ?? result?.defender ?? payload?.defense_id ?? "-";
  const fullText = result?.text ?? payload?.text ?? JSON.stringify(payload);
  const lines = fullText.split('\n');
  let attackContent = "";
  let defenseContent = "";
  lines.forEach(line => {
    const clean = line.trim();
    if (clean.includes("공격 내용") || clean.includes("공격:")) {
      attackContent = clean.replace(/공격\s*내용\s*[:：]\s*/, "").replace(/공격\s*[:：]\s*/, "").trim();
    } else if (clean.includes("방어 내용") || clean.includes("방어:")) {
      defenseContent = clean.replace(/방어\s*내용\s*[:：]\s*/, "").replace(/방어\s*[:：]\s*/, "").trim();
    }
  });
  if (attackContent && attack_id !== "-") attachSummaryByQueue(attack_id, attackContent, "battle");
  if (defenseContent && defense_id !== "-") attachSummaryByQueue(defense_id, defenseContent, "battle");
  if (!attackContent && !defenseContent && attack_id !== "-") attachSummaryByQueue(attack_id, fullText, "battle");
});

stt.onResultSummary((payload: any) => {
  try {
    analyzeAIResponse(payload);

    if (isVoteDecided(payload)) {
      const data = extractVoteDecidedData(payload);
      messages.value.push({
        id: messageIdCounter++,
        type: "result",
        resultType: "vote_decided",
        timestamp: new Date(),
        num1Summary: data.num1Summary,
        num2Summary: data.num2Summary,
        summaryText: `투표 승부 결과 요약`,
      });
    } else if (isAIJudgment(payload)) {
      const data = extractAIJudgmentData(payload);
      messages.value.push({
        id: messageIdCounter++,
        type: "result",
        resultType: "ai_judgment",
        timestamp: new Date(),
        aiWinner: data.aiWinner,               // 'num1' | 'num2' | 'tie'
        originalVotes: data.originalVotes,     // 사람 투표(표시는 안 함)
        aiVotes: data.aiVotes,                 // ✅ 화면에 그대로 표시
        judgmentReason: data.judgmentReason,
        num1Summary: data.num1Summary,
        num2Summary: data.num2Summary,
        summaryText: `AI 최종 판정`,
      });
    } else {
      messages.value.push({
        id: messageIdCounter++,
        type: "result",
        timestamp: new Date(),
        summaryText: `알 수 없는 AI 응답: ${JSON.stringify(payload)}`,
      });
    }
  } catch (error: any) {
    messages.value.push({
      id: messageIdCounter++,
      type: "result",
      timestamp: new Date(),
      summaryText: `AI 응답 처리 오류: ${error.message}\n원본 데이터: ${JSON.stringify(payload)}`,
    });
  }
});

const isRec = computed(() => !!stt.isRecognizing.value);

// WebRTC
const {
  state,
  connectionError,
  allParticipantsConnected,
  stepDone,
  startWebRTCConnection,
  disconnectWebRTC,
  replaceLocalAudioTrack,
  recreateAudioProducerWithTrack,
} = useWebRTCConnection({
  audioController: audioControls,
  participantsCount: roomStore.room?.participants.length ?? 1,
  stompClient: createStompClientWrapper(client.value),
  userInfo: {
    email: currentUserEmail,
    nickname: authStore.userNickname || myParticipant.value?.displayName || '익명',
    isLoggedIn: true,
  },
  enableSend: !isViewer,
});

const connectionStep = computed(() => state.value.connectionStep);

// 토론 주제/정보
const debateSubject = ref(roomStore.room?.topic || "이곳에 토론 주제가 들어갑니다.");
const debateLeftTeam = ref(roomStore.room?.leftTeamName || "좌측 진영");
const debateRightTeam = ref(roomStore.room?.rightTeamName || "우측 진영");
const debateStartAt = ref("");
const currentTime = ref(0);

// UI/단계
const isDebateInfoCollapsed = ref(false);
const stages = ["진영논리", "1차 요약", "공방전", "투표", "토론결과"];
const currentStageIndex = ref(0);
const currentStage = ref(stages[currentStageIndex.value]);

// 발언
const currentSpeaker = ref("");
const nextSpeaker = ref("");
const speakerStartAt = ref("");
const speakingTimeLeft = ref(0);
const isSpeakingStarted = ref(false);
const speakingTimer = ref<number | null>(null);
const speakingDuration = ref(15 * 1000);
const isLeftSpeaking = ref(false);
const isRightSpeaking = ref(false);

// 공방/모달
const isSelectTarget = ref(false);
const selectTargetStartAt = ref("");
const SELECT_TARGET_DURATION = 15 * 1000;
const selectTargetTimeLeft = ref(0);
const selectTargetTimeLeftTimer = ref<number | null>(null);
const selectedPrevTarget = ref<{ userId: string; displayName: string } | null>(null);
const selectedCurrentTarget = ref<{ userId: string; displayName: string } | null>(null);
const selectTargetConfirmed = ref(false);
const isAttackPhase = ref(false);
const isDefensePhase = ref(false);

// 투표
const voteTeam = ref<number | null>(null);
const isVoteTime = ref(false);
const voteStartAt = ref("");
const VOTE_DURATION = 15 * 1000;
const voteTimeLeft = ref(0);
const voteTimer = ref<number | null>(null);
const isVoteDisabled = ref(false);
const voteResult = ref<number | null>(null);
const voteInfo = ref<Record<string, number>>({});
const isTie = ref(false);
const isLoadingAIMessage = ref(true);

// 참가자 상태(공/수)
const participantStates = ref<Record<string, "attack" | "defense" | null>>({});

// 메시지
const messages = ref<Array<{
  id: number;
  type: string;
  mode?: "normal" | "battle";
  isAttacker?: boolean;
  isDefender?: boolean;
  sender?: string;
  senderDisplayName?: string;
  timestamp?: Date;
  team?: number;
  profileImage?: string;

  // vote
  voteInfo?: Record<string, number>;
  voteResult?: number;

  // ai (타이 브레이커 구형 블록 호환)
  aiInfo?: Record<string, number>;

  // stt/summary
  sttText?: string;
  summaryText?: string;
  display?: "stt" | "summary";
  summaryPending?: boolean;
  turnKey?: string;

  // ★ result용 필드 추가
  resultType?: "vote_decided" | "ai_judgment";
  aiWinner?: "num1" | "num2" | "tie" | null;
  originalVotes?: { num1: number; num2: number };
  aiVotes?: { num1: number; num2: number } | null; // ✅ 여기 추가
  aiScores?: { num1: number; num2: number };       // (남겨둬도 되지만 UI에선 안 씀)
  judgmentReason?: string;
  details?: any;
  num1Summary?: string;
  num2Summary?: string;
}>>([]);
const messagesContainer = ref<HTMLElement>();

// STT 표시
const displayedText = (m: any) => m.display === "summary" && m.summaryText ? m.summaryText : m.sttText;
const toggleDisplay = (m: any) => { if (m.summaryText) m.display = m.display === "summary" ? "stt" : "summary"; };

// 준비시간
const preparationTimeLeft = ref(10);
const isPreparationTime = ref(true);
const isTimerStarted = ref(false);
const preparationTimer = ref<number | null>(null);
const PREPARATION_DURATION = 15 * 1000;

// 전환시간
const speakingTransitionTimeLeft = ref(0);
const speakingTransitionTimer = ref<number | null>(null);
const SPEAKING_TRANSITION_DURATION = 4 * 1000;
const speakerEndAt = ref("");
const isTransitionStarted = ref(false);

// 공통: 전환 뷰 즉시 종료
const stopTransitionNow = () => {
  if (speakingTransitionTimer.value) {
    clearInterval(speakingTransitionTimer.value);
    speakingTransitionTimer.value = null;
  }
  isTransitionStarted.value = false;
  speakingTransitionTimeLeft.value = 0;
};

// 발언 타이머(클라 now 기준)
const startSpeakingTimer = () => {
  if (speakingTimer.value) { clearInterval(speakingTimer.value); speakingTimer.value = null; }
  const TOTAL = speakingDuration.value;
  const baseline = Date.now(); // ★ 서버 시각 무시

  isSpeakingStarted.value = true;

  const tick = () => {
    const elapsed = Math.max(0, Date.now() - baseline);
    const remainingMs = Math.max(0, TOTAL - elapsed);
    speakingTimeLeft.value = Math.ceil(remainingMs / 1000);
    if (remainingMs <= 0) {
      clearInterval(speakingTimer.value!);
      speakingTimer.value = null;
      isSpeakingStarted.value = false;
    }
  };

  tick();
  speakingTimer.value = window.setInterval(tick, 1000);
};

// 공격 대상 선택 타이머(클라 now 기준)
const startSelectTargetTimer = () => {
  if (selectTargetTimeLeftTimer.value) { clearInterval(selectTargetTimeLeftTimer.value); selectTargetTimeLeftTimer.value = null; }
  const TOTAL = SELECT_TARGET_DURATION;
  const baseline = Date.now(); // ★

  isSelectTarget.value = true;

  const tick = () => {
    const elapsed = Math.max(0, Date.now() - baseline);
    const remainingMs = Math.max(0, TOTAL - elapsed);
    selectTargetTimeLeft.value = Math.ceil(remainingMs / 1000);
    if (remainingMs <= 0) {
      clearInterval(selectTargetTimeLeftTimer.value!);
      selectTargetTimeLeftTimer.value = null;
      setTimeout(() => {
        isSelectTarget.value = false;
        selectedCurrentTarget.value = null;
        selectedPrevTarget.value = null;
      }, 2000);
    }
  };

  tick();
  selectTargetTimeLeftTimer.value = window.setInterval(tick, 1000);
};

// 투표 타이머(클라 now 기준)
const startVoteTimer = () => {
  if (voteTimer.value) { clearInterval(voteTimer.value); voteTimer.value = null; }
  const TOTAL = VOTE_DURATION;
  const baseline = Date.now(); // ★

  isVoteTime.value = true;

  const tick = () => {
    const elapsed = Math.max(0, Date.now() - baseline);
    const remainingMs = Math.max(0, TOTAL - elapsed);
    voteTimeLeft.value = Math.ceil(remainingMs / 1000);
    if (remainingMs <= 0) {
      clearInterval(voteTimer.value!);
      voteTimer.value = null;
      isVoteTime.value = false;
    }
  };

  tick();
  voteTimer.value = window.setInterval(tick, 1000);
};

// 발언 전환 타이머(클라 now 기준)
const startSpeakingTransitionTimer = () => {
  if (speakingTransitionTimer.value) { clearInterval(speakingTransitionTimer.value); speakingTransitionTimer.value = null; }
  const TOTAL = SPEAKING_TRANSITION_DURATION;
  const baseline = Date.now(); // ★

  isTransitionStarted.value = true;

  const tick = () => {
    const elapsed = Math.max(0, Date.now() - baseline);
    const remainingMs = Math.max(0, TOTAL - elapsed);
    speakingTransitionTimeLeft.value = Math.ceil(remainingMs / 1000);
    if (remainingMs <= 0) {
      clearInterval(speakingTransitionTimer.value!);
      speakingTransitionTimer.value = null;
      isTransitionStarted.value = false;
    }
  };

  tick();
  speakingTransitionTimer.value = window.setInterval(tick, 1000);
};

// 준비시간 타이머(클라 now 기준)
const startPreparationTimer = () => {
  if (preparationTimer.value) { clearInterval(preparationTimer.value); preparationTimer.value = null; }
  const TOTAL = PREPARATION_DURATION;
  const baseline = Date.now(); // ★

  isTimerStarted.value = true;

  const tick = () => {
    const elapsed = Math.max(0, Date.now() - baseline);
    const remainingMs = Math.max(0, TOTAL - elapsed);
    preparationTimeLeft.value = Math.ceil(remainingMs / 1000);
    if (remainingMs <= 0) {
      clearInterval(preparationTimer.value!);
      preparationTimer.value = null;
      // 메인 화면으로 넘어가는 것은 speak/start 수신 시점에 일괄 처리
    }
  };

  tick();
  preparationTimer.value = window.setInterval(tick, 1000);
};

// 모달 제어
const handleSelectTargetModalClose = (newValue: boolean) => {
  if (selectTargetTimeLeft.value > 0) return;
  isSelectTarget.value = newValue;
};
const handleVoteModalClose = (newValue: boolean) => {
  if (voteTimeLeft.value > 0) return;
  isVoteTime.value = newValue;
};

// 기타 유틸
const setParticipantAudioRef = (userEmail: string, el: any) => {
  if (el && el instanceof HTMLAudioElement) {
    audioControls.setParticipantAudio(userEmail, el);
  }
};
const isParticipantMuted = (userId: string) => {
  const muted = audioControls.getParticipantMuted(userId);
  const volume = audioControls.getParticipantVolume(userId);
  return muted || volume <= 0;
};

// 선택/공격
const selectTarget = () => {
  if (!selectedCurrentTarget.value) return;
  if (selectedPrevTarget.value?.userId === selectedCurrentTarget.value?.userId) return;
  selectedPrevTarget.value = selectedCurrentTarget.value;
  debateClient.value?.publish({
    destination: `/pub/debate/attack`,
    body: JSON.stringify({
      roomId: roomStore.room?.roomId,
      target: selectedCurrentTarget.value?.userId,
    }),
  });
};
const selectTargetParticipant = (participant: { userId: string; displayName: string }) => {
  selectedCurrentTarget.value = participant;
  selectTarget();
};
const updateSelectedTarget = (msg: DebateSelectTargetResponse) => {
  if (msg.attacker === debateStore.myEmail) selectTargetConfirmed.value = true;
};

// 진영별 computed
const leftTeam = computed(() => roomStore.leftTeam);
const rightTeam = computed(() => roomStore.rightTeam);
const selectedRows = computed(() => {
  const n = Math.max(leftTeam.value.length, rightTeam.value.length);
  return Array.from({ length: n }, (_, i) => ({
    left: leftTeam.value[i] ?? null,
    right: rightTeam.value[i] ?? null,
  }));
});
const wrapName = (name: string) => name.length > 8 ? name.slice(0, 8) + "\n" + name.slice(8) : name;
const attackersFor = (userId: string) => targetSelectionStore.attackersFor(userId);

// 투표
const vote = async (userId: string, side: number) => {
  voteTeam.value = side;
  isVoteDisabled.value = true;
  const response = await fetch(
    `/api/debate/rooms/${roomStore.room?.roomId}/vote`,
    {
      method: "POST",
      mode: "cors",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ userEmail: userId, team: side }),
    }
  );
  await response.json().catch(() => {});
};

// 발언 순서 표시용
const speakingOrderMap = computed(() => {
  const map: Record<string, number> = {};
  const maxLength = Math.max(roomStore.leftTeam.length, roomStore.rightTeam.length);
  let order = 1;
  for (let i = 0; i < maxLength; i++) {
    if (i < roomStore.leftTeam.length) map[roomStore.leftTeam[i].userId] = order++;
    if (i < roomStore.rightTeam.length) map[roomStore.rightTeam[i].userId] = order++;
  }
  return map;
});

// 시청자 채팅
const audienceMessages = ref<any[]>([]);
const audienceChatContainer = ref<HTMLElement>();
const audienceChatScrollArea = ref<any>();
const audienceCount = ref(42);
const newChatMessage = ref("");
const sendChatMessage = () => {
  if (newChatMessage.value.trim() === "") return;
  debateClient.value.publish({
    destination: `/pub/debate/chat`,
    body: JSON.stringify({
      roomId: roomStore.room?.roomId,
      nickname: authStore.userNickname || "익명",
      message: newChatMessage.value,
    }),
  });
  newChatMessage.value = "";
};

// 메시지 ID
let messageIdCounter = 0;
let audienceMessageIdCounter = 0;

// STOMP 연결
const debateClient = ref<any>(null);
const connectToDebateRoom = async () => {
  return new Promise((resolve, reject) => {
    try {
      debateClient.value = new Client({
        brokerURL: `/api/debate/ws`,
        debug: (str) => console.log("STOMP Debug:", str),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: (frame) => resolve(debateClient.value),
        onDisconnect: () => {},
        onStompError: (frame) => reject(new Error(`STOMP 오류: ${frame.headers.message}`)),
        onWebSocketError: (error) => reject(error),
      });
      debateClient.value.activate();
    } catch (error) {
      reject(error);
    }
  });
};

// 구독
const subscribeToDebateRoom = (client: Client) => {
  if (!client) return;

  // 준비 시작(오피니언)
  client.subscribe(
    `/sub/debate/room/${roomStore.room?.roomId}/start/opinion`,
    (message) => {
      const msg = JSON.parse(message.body) as DebateStartMessage;
      stepDone.completed.resolve();
      debateSubject.value = msg.topicText;
      debateLeftTeam.value = msg.firstOption;
      debateRightTeam.value = msg.secondOption;
      debateStartAt.value = withKstIfNaive(msg.debateStartAt);
      // 준비 타이머는 now 기준
      startPreparationTimer();
    }
  );

  // 오피니언 발언 시작
  client.subscribe(
    `/sub/debate/room/${roomStore.room?.roomId}/speak/start`,
    (message) => {
      const msg = JSON.parse(message.body) as DebateSpeakStartMessage;
      stopTransitionNow(); // ★ 전환 종료

      currentSpeaker.value = msg.speaker;
      speakerStartAt.value = new Date().toISOString(); // ★ now
      currentStageIndex.value = 0;
      currentStage.value = stages[currentStageIndex.value];
      speakingDuration.value = 15 * 1000;

      if (roomStore.leftTeam.find((p) => p.userId === currentSpeaker.value)) {
        isLeftSpeaking.value = true;
        isRightSpeaking.value = false;
      } else {
        isRightSpeaking.value = true;
        isLeftSpeaking.value = false;
      }

      isPreparationTime.value = false; // ★ 메인화면 강제 전환
      nextSpeaker.value = "";
      if (currentSpeaker.value === debateStore.myEmail) stt.startOpinion();
      startSpeakingTimer(); // ★ now baseline
    }
  );

  // 오피니언 발언 종료
  client.subscribe(
    `/sub/debate/room/${roomStore.room?.roomId}/speak/end`,
    (message) => {
      const msg = JSON.parse(message.body) as DebateSpeakEndMessage;
      speakerEndAt.value = withKstIfNaive(msg.speakerEndAt);

      if (currentSpeaker.value === debateStore.myEmail) stt.stop();
      if (speakingTimer.value) { clearInterval(speakingTimer.value); speakingTimer.value = null; }
      isSpeakingStarted.value = false;

      currentSpeaker.value = "";
      isLeftSpeaking.value = false;
      isRightSpeaking.value = false;

      nextSpeaker.value = msg.nextSpeaker;
      startSpeakingTransitionTimer(); // ★ now baseline
    }
  );

  // 공방 시작(타겟 선택)
  client.subscribe(
    `/sub/debate/room/${roomStore.room?.roomId}/start/battle`,
    (message) => {
      const msg = JSON.parse(message.body) as DebateSelectTargetMessage;
      isPreparationTime.value = false; // ★ 메인화면 보장
      stopTransitionNow();            // ★ 전환 종료
      isSelectTarget.value = true;
      selectTargetStartAt.value = new Date().toISOString(); // ★ now
      currentStageIndex.value = 2;
      currentStage.value = stages[currentStageIndex.value];
      startSelectTargetTimer(); // ★ now baseline
    }
  );

  // 타겟 선택 결과
  client.subscribe(
    `/sub/debate/room/${roomStore.room?.roomId}/attack`,
    (message) => {
      const msg = JSON.parse(message.body) as DebateSelectTargetResponse;
      if (msg.attacker === debateStore.myEmail) selectTargetConfirmed.value = true;
      targetSelectionStore.applySelect(msg);
    }
  );

  // 공격 발언 시작
  client.subscribe(
    `/sub/debate/room/${roomStore.room?.roomId}/speak/attackStart`,
    (message) => {
      const msg = JSON.parse(message.body) as DebateSpeakStartMessage;
      isSelectTarget.value = false;
      stopTransitionNow(); // ★

      currentSpeaker.value = msg.speaker;
      speakerStartAt.value = new Date().toISOString(); // ★ now
      currentStageIndex.value = 2;
      currentStage.value = stages[currentStageIndex.value];
      speakingDuration.value = 15 * 1000;
      isAttackPhase.value = true;
      participantStates.value[msg.speaker] = "attack";

      if (roomStore.leftTeam.find((p) => p.userId === currentSpeaker.value)) {
        isLeftSpeaking.value = true;
        isRightSpeaking.value = false;
      } else {
        isRightSpeaking.value = true;
        isLeftSpeaking.value = false;
      }

      nextSpeaker.value = "";
      if (currentSpeaker.value === debateStore.myEmail) stt.startBattleAttack();
      startSpeakingTimer();
    }
  );

  // 공격 발언 종료
  client.subscribe(
    `/sub/debate/room/${roomStore.room?.roomId}/speak/attackEnd`,
    (message) => {
      const msg = JSON.parse(message.body) as DebateSpeakEndMessage;
      speakerEndAt.value = withKstIfNaive(msg.speakerEndAt);
      if (currentSpeaker.value === debateStore.myEmail) stt.stop();
      if (speakingTimer.value) { clearInterval(speakingTimer.value); speakingTimer.value = null; }
      isSpeakingStarted.value = false;
      if (currentSpeaker.value) participantStates.value[currentSpeaker.value] = null;
      currentSpeaker.value = "";
      isLeftSpeaking.value = false;
      isRightSpeaking.value = false;
      isAttackPhase.value = false;
      nextSpeaker.value = msg.nextSpeaker;
      startSpeakingTransitionTimer();
    }
  );

  // 방어 발언 시작
  client.subscribe(
    `/sub/debate/room/${roomStore.room?.roomId}/speak/defenseStart`,
    (message) => {
      const msg = JSON.parse(message.body) as DebateSpeakStartMessage;
      stopTransitionNow(); // ★

      currentSpeaker.value = msg.speaker;
      speakerStartAt.value = new Date().toISOString(); // ★ now
      currentStageIndex.value = 2;
      currentStage.value = stages[currentStageIndex.value];
      speakingDuration.value = 15 * 1000;
      isDefensePhase.value = true;
      participantStates.value[msg.speaker] = "defense";

      if (roomStore.leftTeam.find((p) => p.userId === currentSpeaker.value)) {
        isLeftSpeaking.value = true;
        isRightSpeaking.value = false;
      } else {
        isRightSpeaking.value = true;
        isLeftSpeaking.value = false;
      }

      nextSpeaker.value = "";
      if (currentSpeaker.value === debateStore.myEmail) stt.startBattleDefense();
      startSpeakingTimer();
    }
  );

  // 방어 발언 종료
  client.subscribe(
    `/sub/debate/room/${roomStore.room?.roomId}/speak/defenseEnd`,
    (message) => {
      const msg = JSON.parse(message.body) as DebateSpeakEndMessage;
      speakerEndAt.value = withKstIfNaive(msg.speakerEndAt);
      if (currentSpeaker.value === debateStore.myEmail) stt.stop();
      if (speakingTimer.value) { clearInterval(speakingTimer.value); speakingTimer.value = null; }
      isSpeakingStarted.value = false;
      if (currentSpeaker.value) participantStates.value[currentSpeaker.value] = null;
      currentSpeaker.value = "";
      isLeftSpeaking.value = false;
      isRightSpeaking.value = false;
      isDefensePhase.value = false;
      nextSpeaker.value = msg.nextSpeaker;
      startSpeakingTransitionTimer();
    }
  );

  // 투표 시작
  client.subscribe(
    `/sub/debate/room/${roomStore.room?.roomId}/vote/start`,
    (message) => {
      const msg = JSON.parse(message.body) as DebateVoteStartMessage;
      isPreparationTime.value = false; // ★ 메인화면 보장
      stopTransitionNow();            // ★ 전환 종료
      isVoteTime.value = true;
      voteStartAt.value = new Date().toISOString(); // ★ now
      voteTimeLeft.value = Math.ceil(VOTE_DURATION / 1000);
      currentStageIndex.value = 3;
      currentStage.value = stages[currentStageIndex.value];
      nextSpeaker.value = "";
      startVoteTimer();
    }
  );

  // 투표 종료
  client.subscribe(
    `/sub/debate/room/${roomStore.room?.roomId}/vote/end`,
    (message) => {
      const msg = JSON.parse(message.body) as DebateVoteEndMessage;
      isVoteTime.value = false;
      voteTimeLeft.value = 0;
      currentStageIndex.value = 4;
      currentStage.value = stages[currentStageIndex.value];
      voteTeam.value = null;
      isVoteDisabled.value = false;
      voteInfo.value = msg.voteInfo;
      voteResult.value = msg.voteResult;
      if (msg.voteResult === 2) isTie.value = true;
      messages.value.push({
        id: messageIdCounter++,
        type: "vote",
        voteResult: msg.voteResult,
        voteInfo: msg.voteInfo,
        timestamp: new Date(),
      });
    }
  );

  // 채팅
  client.subscribe(
    `/sub/debate/room/${roomStore.room?.roomId}/chat`,
    (message) => {
      const msg = JSON.parse(message.body) as DebateChatMessage;
      audienceMessages.value.push({
        id: audienceMessageIdCounter++,
        text: msg.message,
        nickname: msg.nickname,
        timestamp: new Date(),
        team: msg.team,
      });
    }
  );
};

// 마운트
onMounted(async () => {
  await connectToDebateRoom();
  stompRef.value = debateClient.value;
  stt.subscribe();
  subscribeToDebateRoom(debateClient.value!);
  await startWebRTCConnection(roomStore.room?.roomId ?? "");
  debateClient.value.publish({
    destination: `/pub/debate/room/${roomStore.room?.roomId}/join`,
    body: JSON.stringify({ roomId: roomStore.room?.roomId }),
  });
});

// 언마운트
onUnmounted(() => {
  [speakingTimer, speakingTransitionTimer, selectTargetTimeLeftTimer, voteTimer, preparationTimer]
    .forEach(t => { if (t.value) clearInterval(t.value); });
  stt.unsubscribe?.();
  disconnectWebRTC();
  debateStore.resetDebateState();
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
