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
        class="absolute inset-0 z-50 bg-black bg-opacity-90 flex items-center justify-center"
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
                <div class="text-2xl text-debate-left mb-4 font-bold">
                  좌측 진영
                </div>
                <div class="flex justify-center space-x-2">
                  <div
                    v-for="participant in roomStore.leftTeam"
                    :key="participant.userId"
                    class="flex flex-col items-center space-y-2 w-32"
                  >
                    <div class="relative">
                      <Avatar class="w-16 h-16">
                        <AvatarImage :src="debateLeftProfile" alt="좌측 진영" />
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
                      class="text-sm text-white text-center font-bold max-w-full break-words"
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
                <div class="text-2xl text-debate-right mb-4 font-bold">
                  우측 진영
                </div>
                <div class="flex justify-center space-x-2">
                  <div
                    v-for="participant in roomStore.rightTeam"
                    :key="participant.userId"
                    class="flex flex-col items-center space-y-2 w-32"
                  >
                    <div class="relative">
                      <Avatar class="w-16 h-16">
                        <AvatarImage
                          :src="debateRightProfile"
                          alt="우측 진영"
                        />
                      </Avatar>
                      <!-- 연결 상태 표시 -->
                      <div
                        :class="[
                          'absolute -bottom-1 -right-1 w-6 h-6 rounded-full border-2 border-white flex items-center justify-center',
                          'bg-gray-400',
                        ]"
                      >
                        <div
                          class="w-3 h-3 bg-white rounded-full animate-pulse"
                        ></div>
                      </div>
                    </div>
                    <span
                      class="text-sm text-white text-center max-w-full break-words font-bold"
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
                      연결 중...
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
          <div class="text-9xl font-bold text-white mb-8 animate-pulse">
            {{ preparationTimeLeft }}
          </div>

          <!-- 준비시간 텍스트 -->
          <div class="text-3xl text-white mb-4">준비시간</div>

          <!-- 설명 텍스트 -->
          <div class="text-lg text-gray-300 mb-8">
            토론이 곧 시작됩니다. 마음의 준비를 하세요!
          </div>

          <!-- 프로그레스 바 -->
          <div class="w-96 bg-gray-700 rounded-full h-3 mx-auto mb-8">
            <div
              class="bg-blue-500 h-3 rounded-full transition-all duration-1000 ease-linear"
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
                <div class="text-2xl font-bold text-debate-left mb-4">
                  {{ debateLeftTeam }}
                </div>
                <div class="flex justify-center space-x-2">
                  <div
                    v-for="participant in roomStore.leftTeam"
                    :key="participant.userId"
                    class="flex flex-col items-center space-y-2 w-32"
                  >
                    <Avatar class="w-16 h-16">
                      <AvatarImage
                        :src="debateLeftProfile"
                        :alt="participant.displayName"
                      />
                    </Avatar>
                    <span
                      class="text-sm text-white text-center max-w-full break-words font-bold"
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
                    <span class="text-xs px-2 py-1 text-gray-300">
                      {{ speakingOrderMap[participant.userId] }}번째
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
                <div class="text-2xl font-bold text-debate-right mb-4">
                  {{ debateRightTeam }}
                </div>
                <div class="flex justify-center space-x-2">
                  <div
                    v-for="participant in roomStore.rightTeam"
                    :key="participant.userId"
                    class="flex flex-col items-center space-y-2 w-32"
                  >
                    <Avatar class="w-16 h-16">
                      <AvatarImage
                        :src="debateRightProfile"
                        :alt="participant.displayName"
                      />
                    </Avatar>
                    <span
                      class="text-sm text-white text-center max-w-full break-words font-bold"
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
                    <span class="text-xs px-2 py-1 text-gray-300">
                      {{ speakingOrderMap[participant.userId] }}번째
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
                    다음 발언자는
                    {{ currentSpeaker ? currentSpeaker : "준비 중..." }}입니다.
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
                class="bg-debate-left transition-all duration-700 ease-in-out border-debate-left text-black gap-2"
                style="transition-delay: 300ms"
              >
                <CardHeader>
                  <CardTitle class="text-2xl text-center">{{
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
                      <Avatar
                        :class="[
                          'w-16 h-16 relative overflow-visible',
                          // participant.id === debateSession.currentSpeakerId
                          //   ? 'speaking-glow'
                          //   : '',
                        ]"
                      >
                        <AvatarImage
                          :src="debateLeftProfile"
                          :alt="participant.displayName"
                        />
                        <!-- 공격/수비 아이콘 -->
                        <div
                          v-if="participantStates[participant.userId]"
                          class="absolute -top-8 z-10 w-16 h-16 flex items-center justify-center"
                        >
                          <img
                            :src="
                              participantStates[participant.userId] === 'attack'
                                ? attackIcon
                                : defenseIcon
                            "
                            :alt="
                              participantStates[participant.userId] === 'attack'
                                ? '공격'
                                : '수비'
                            "
                            class="w-8 h-8"
                          />
                        </div>
                      </Avatar>
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
                      {{ speakingTimeLeft || 0 }}초
                    </div>
                    <!-- 프로그레스 바 -->
                    <div class="w-full bg-gray-800 rounded-full h-3 mx-auto">
                      <div
                        class="bg-blue-500 h-3 rounded-full transition-all duration-1000 ease-linear"
                        :style="{
                          width: `${((speakingTimeLeft || 0) / (speakingDuration / 1000)) * 100}%`,
                        }"
                      ></div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <!-- 우측 진영 정보 -->
              <Card
                class="bg-debate-right transition-all duration-700 ease-in-out border-debate-right text-white gap-2"
                style="transition-delay: 300ms"
              >
                <CardHeader class="p-0">
                  <CardTitle class="text-2xl text-center">{{
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
                      <Avatar
                        :class="[
                          'w-16 h-16 relative overflow-visible',
                          // participant.id === debateSession.currentSpeakerId
                          //   ? 'speaking-glow'
                          //   : '',
                        ]"
                      >
                        <AvatarImage
                          :src="debateRightProfile"
                          :alt="participant.displayName"
                        />
                        <!-- 공격/수비 아이콘 -->
                        <div
                          v-if="participantStates[participant.userId]"
                          class="absolute -top-8 z-10 w-16 h-16 flex items-center justify-center"
                        >
                          <img
                            :src="
                              participantStates[participant.userId] === 'attack'
                                ? attackIcon
                                : defenseIcon
                            "
                            :alt="
                              participantStates[participant.userId] === 'attack'
                                ? '공격'
                                : '수비'
                            "
                            class="w-8 h-8"
                          />
                        </div>
                      </Avatar>
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
                      {{ speakingTimeLeft || 0 }}초
                    </div>
                    <!-- 프로그레스 바 -->
                    <div class="w-full bg-white rounded-full h-3 mx-auto">
                      <div
                        class="bg-blue-500 h-3 rounded-full transition-all duration-1000 ease-linear"
                        :style="{
                          width: `${(speakingTimeLeft || 0 / (speakingDuration / 1000)) * 100}%`,
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
        <Card class="flex flex-col min-h-0">
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
                    message.team === 'left' ? 'left-0' : 'right-0',
                  ]"
                  style="width: 100px"
                >
                  <!-- 프로필 이미지 -->
                  <Avatar
                    :class="'w-[min(6vw,64px)] h-[min(6vw,64px)] relative overflow-visible'"
                  >
                    <AvatarImage
                      :src="debateLeftProfile"
                      :alt="message.sender"
                    />
                    <div
                      class="absolute -top-8 z-10 w-16 h-16 flex items-center justify-center"
                    >
                      <img
                        :src="
                          message.team === 'left' ? attackIcon : defenseIcon
                        "
                        :alt="message.team === 'left' ? '공격' : '수비'"
                        class="w-8 h-8"
                      />
                    </div>
                  </Avatar>

                  <!-- 발언자 이름 (8글자 넘으면 개행) -->
                  <div
                    class="text-xs font-semibold text-slate-700 text-center leading-tight"
                    style="width: 100px"
                  >
                    <span v-if="message.sender.length <= 8">{{
                      message.sender
                    }}</span>
                    <span v-else class="block">
                      {{ message.sender.substring(0, 8) }}<br />{{
                        message.sender.substring(8)
                      }}
                    </span>
                  </div>
                </div>

                <!-- 말풍선 영역 (프로필 영역 피해서) -->
                <div
                  :class="[
                    'relative p-8 rounded-4xl text-lg shadow-sm',
                    message.team === 'left'
                      ? 'bg-debate-left text-slate-800 ml-28 mr-28'
                      : 'bg-debate-right text-white mr-28 ml-28',
                  ]"
                  style="min-height: 80px; display: flex; align-items: center"
                >
                  <!-- 말풍선 꼬리 -->
                  <div
                    v-if="message.team === 'left'"
                    class="absolute top-6 left-[-10px] w-0 h-0 border-t-[12px] border-b-[12px] border-r-[12px] border-t-transparent border-b-transparent border-r-debate-left"
                  ></div>
                  <div
                    v-if="message.team === 'right'"
                    class="absolute top-6 right-[-10px] w-0 h-0 border-t-[12px] border-b-[12px] border-l-[12px] border-t-transparent border-b-transparent border-l-debate-right"
                  ></div>
                  {{ message.text }}
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
      <Dialog :open="isSelectTarget" @update:open="handleModalClose">
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
                    ? 'shadow-sm bg-blue-400'
                    : 'border-gray-200 dark:border-gray-700 hover:bg-blue-300 dark:hover:bg-blue-300 hover:shadow-md hover:-translate-y-0.5',
                ]"
              >
                <Avatar class="w-10 h-10">
                  <AvatarImage
                    :src="debateLeftProfile"
                    :alt="participant.displayName"
                  />
                </Avatar>
                <div class="flex-1 min-w-0">
                  <div class="font-medium text-sm truncate">
                    {{ participant.displayName }}
                  </div>
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
                    ? 'shadow-sm bg-blue-400'
                    : 'border-gray-200 dark:border-gray-700 hover:bg-blue-300 dark:hover:bg-blue-300 hover:shadow-md hover:-translate-y-0.5',
                ]"
              >
                <Avatar class="w-10 h-10">
                  <AvatarImage
                    :src="debateRightProfile"
                    :alt="participant.displayName"
                  />
                </Avatar>
                <div class="flex-1 min-w-0">
                  <div class="font-medium text-sm truncate">
                    {{ participant.displayName }}
                  </div>
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
                    <Avatar class="w-16 h-16 overflow-visible">
                      <AvatarImage
                        :src="debateLeftProfile"
                        :alt="row.left.displayName"
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
                    <Avatar class="w-16 h-16 overflow-visible">
                      <AvatarImage
                        :src="debateRightProfile"
                        :alt="row.right.displayName"
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
              <div class="text-xs">공격 대상 선택 시간</div>
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
                    'h-3 rounded-full transition-all duration-500 ease-linear',
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
          <Card class="h-full flex flex-col">
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
                      <AvatarImage :src="audienceProfile" alt="시청자" />
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

const client = ref<Client | null>(null);
const route = useRoute();

// Debate Store 사용
const debateStore = useDebateStore();
const roomStore = useRoomStore();
const authStore = useAuthStore();
const targetSelectionStore = useTargetSelectionStore();
const audioControls = useAudioControls();

targetSelectionStore.applySelect({
  attacker: "user01@test.com",
  defender: "user02@test.com",
});

targetSelectionStore.applySelect({
  attacker: "user02@test.com",
  defender: "user01@test.com",
});

targetSelectionStore.applySelect({
  attacker: "user04@test.com",
  defender: "user01@test.com",
});
targetSelectionStore.applySelect({
  attacker: "user03@test.com",
  defender: "user04@test.com",
});

// 참가자별 오디오 엘리먼트 참조 설정
const setParticipantAudioRef = (userEmail: string, el: any) => {
  if (el && el instanceof HTMLAudioElement) {
    audioControls.setParticipantAudio(userEmail, el);
    console.log(`🎵 ${userEmail}의 오디오 엘리먼트 설정됨`);
  }
};

// 커스텀 방에서 진입 시 방 정보가 이미 세팅되었을 수 있으므로, 비어있을 때만 기본값 주입
if (!roomStore.room) {
  roomStore.setRoom({
    roomId: "11",
    participants: [
      { userId: "user01@test.com", displayName: "김정택", side: "L" },
      {
        userId: "user02@test.com",
        displayName: "권우상권우상권우권우상권우상권우",
        side: "R",
      },
      { userId: "user03@test.com", displayName: "김형수", side: "L" },
      { userId: "user04@test.com", displayName: "지준오", side: "R" },
    ],
  });
}

// STT 관련

const sttStompClient = ref<Client | null>(null);
const isSttConnected = ref(false);


const connectStt = () => {
  return new Promise((resolve, reject) => {
    const client = new Client({
      brokerURL: `${import.meta.env.VITE_API_BASE_URL}:8082/ws`, // 8082 포트로 연결
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000
    });

    client.onConnect = () => {
      console.log('🔗 STT WebSocket 연결 성공 (8082)');
      isSttConnected.value = true;
      sttStompClient.value = client;
      resolve(void 0);
    };

    client.onStompError = (error) => {
      console.error('❌ STT WebSocket 연결 실패:', error);
      isSttConnected.value = false;
      reject(error);
    };

    client.activate();
  });
};

const disconnectStt = () => {
  if (sttStompClient.value && isSttConnected.value) {
    sttStompClient.value.deactivate();
    isSttConnected.value = false;
    sttStompClient.value = null;
  }
};

connectStt().catch(console.error);

const {
  isRecognizing,
  previewText,
  start,
  stop: stopStt,
  onSegmentReady,
  onSttMessage
} = useStt(sttStompClient, roomStore.room?.roomId ?? 0);

const joined = ref(false);
watchEffect(() => {
  if (isSttConnected.value && !joined.value && roomId.value) {
    sttStompClient.value?.publish({ destination: '/pub/debate/join', body: String(roomId.value) });
    joined.value = true;
  }
});
watch(isSttConnected, (isConnected)=>{ if(!isConnected) joined.value = false; });

// 로그
const recv = ref<Array<{ ts: number; payload: unknown }>>([]),
  sent = ref<Array<{ ts: number; segment: unknown }>>([]);
onSttMessage((p: any)=> recv.value.unshift({ ts: Date.now(), payload: p }));
onSegmentReady((s: any)=> sent.value.unshift({ ts: Date.now(), segment: s }));

const startOpinion = () => start({ phase: "OPINION" });
const startAttack = () => start({ phase: "BATTLE", isAttack: true });
const startDefense = () => start({ phase: "BATTLE", isAttack: false });
const clear = () => {
  recv.value = [];
  sent.value = [];
};
const fmt = (o: any) => JSON.stringify(o, null, 2);




const firstTeam = ref<Team[]>([]);
const secondTeam = ref<Team[]>([]);

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
} = useWebRTCConnection({
  audioController: audioControls,
  participantsCount: roomStore.room?.participants.length ?? 1,
  stompClient: createStompClientWrapper(client),
});

const connectionStep = computed(() => {
  return state.value.connectionStep;
});

// 토론 주제
const debateSubject = ref(roomStore.room?.topic || "이곳에 토론 주제가 들어갑니다.");
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
const speakerStartAt = ref("");
const speakingTimeLeft = ref(0);
const isSpeakingStarted = ref(false);
const speakingTimer = ref<number | null>(null);
const speakingDuration = ref(60 * 1000);

const speakingOrderMap = computed(() => {
  const map: Record<string, number> = {};
  const maxLength = Math.max(firstTeam.value.length, secondTeam.value.length);
  let order = 1;

  for (let i = 0; i < maxLength; i++) {
    const left = firstTeam.value.find((p) => p.order === i);
    const right = secondTeam.value.find((p) => p.order === i);

    if (left) {
      map[left.user] = order;
      order++;
    }

    if (right) {
      map[right.user] = order;
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

const handleModalClose = (newValue: boolean) => {
  if (selectTargetTimeLeft.value > 0) {
    console.log("모달 닫기 시도가 차단됨 - 아직 제한시간이 남아있습니다.");
    return;
  }

  isSelectTarget.value = newValue;
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

  // 공격 대상 선택 서버로 전송
  client.value?.publish({
    destination: `/pub/debate/attack`,
    body: JSON.stringify({
      roomId: roomId.value,
      target: selectedCurrentTarget.value?.userId,
    }),
  });
};

const startSelectTargetTimer = () => {
  console.log("startSelectTargetTimer");
  const serverStartMs = new Date(selectTargetStartAt.value).getTime();
  currentTime.value = Date.now();
  const elapsedMs = currentTime.value - serverStartMs;
  // const remainingMs = Math.max(SELECT_TARGET_DURATION - elapsedMs, 0);
  const remainingMs = 30000;

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

// 참가자별 공격/수비 상태 관리 (userId는 문자열이므로 string 키 사용)
const participantStates = ref<Record<string, "attack" | "defense" | null>>({});

// 진영별 남은 시간
const leftTeamTime = ref("01:00");
const rightTeamTime = ref("00:00");

// 메시지 관련 변수들
const messages = ref<
  Array<{
    id: number;
    text: string;
    sender: string;
    timestamp: Date;
    team: "left" | "right";
    profileImage: string;
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
const STT_DURATION = 60 * 1000; // 1분

// 준비시간 관련 변수
const preparationTimeLeft = ref(30);
const isPreparationTime = ref(true);
const isTimerStarted = ref(false);
const preparationTimer = ref<number | null>(null);
const PREPARATION_DURATION = 30 * 1000; // 30초

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
const SPEAKING_TRANSITION_DURATION = 3 * 1000;
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
const addTestMessage = () => {
  const testMessages = [
    {
      text: "보도하여 공군을 방향에 월드, 투자에 많다. 손꼽고 칠월과 지금이 아직 크고 하다 자동차의 것 활동은 지적하여 새 북서쪽을, 있다 이후는 건강이라도 짙으라면 금융에 양국도, 몇 되다. 페인트는 의견에게 평균이, 우선 돈에 다듬다 여러 5회 놓은 더 선거는, 형성되다. 개방을 역할인 이곳은 전반도 예상됩니다. 맡지만 집회는 대한다 발부받아 하다 알 늘어 남을, 생각하라. 말 유치원을, 투수는 맞을 가두다. 처분은 있고 부분적에, 요구로 포워드를 피해가 전과 외롭다. 된 숙면이라 거짓까지 힘세나 가족을 공격하다. 세침이 그동안이다 4명, 돈을 이 수수료마저 유발하다 떨어뜨린 짧은 같는 오고, 큰 통계의 낙태죄는 걷다 있다. 하구나 여러 자청하여 부분으로 오른 대하다. 2024년 줄인 보다 계파로 나타나거나 첫 본격적에 범행의 게임은 중과세로 완화하다. 총선에 것 재사용의 자본의 거주다, 수용이 갑자기 일그러지다. 성숙하기 눈치가 수자원에 93메가바이트 통신에 발전이 살다. 군축에 발전되면서 도전은 앞쪽의 인상되라. 도시를 유리되다 개발한 열리어 없이 킬로칼로리 기다리다. 23세 21일 신호등에 아쿠아로빅스를 있은 말하다.",
      sender: leftTeam.value[0].displayName,
      team: "left",
    },
    {
      text: "그 사장은 시작은 여성이 이제는 않은 네 어둡어서 나로 호조에 입다. 난다 자르면 체포는, 인식을 태웁니다 하류로 없이 하나에 냉장고 전해지게 갈다. 말을 것 측 시간이 들리다. 8가지 우리를 그 높은 모기업으로 낮다, 어느 살벌하다. 죽자면 본 재생으로 나아 안 지방이 한식집에 독특하다. 지도로 시대를 느껴지어 부처를 주는 돈이고 즐기다. 우리가 이어서 마구 구필에 상대적, 도입은 허둥거린 검사받은 조성되어야지. 없이 있은 참조는 가지는 회사에 시각화한 신규도 통과도 말하여서 말하다. 앵무새를 앞을 된 혼합이 혈에 게임에, 된다 움츠러들다. 수 쇠고기다 그래서 뜨다, 같는 그러나 변화되더니 본다 그다음이 기름이다 엷다. 반점은 말면 언젠가 보급은 많다나 낸다. 무엇을 아이고 벌이며 세력의 모든 성장시키다 보게, 숨차아 말미암아요. 생산의 회복은 낸 말을 산 진입하여서 진짜, 결과로 갖아 훌륭하다. 줄넘기를 볼 번개의 나에 결의하게 거기에 것 위하다. 것 집에서 보는데 하여간 탐독한 줄까 국산화하고 지난해와 포기하여요. 소금보다 시비에 또는 혜안도, 없다.",
      sender: rightTeam.value[0].displayName,
      team: "right",
    },
  ];

  const message = testMessages[testMessageIndex];
  const profileImage =
    message.team === "left" ? debateLeftProfile : debateRightProfile;

  messages.value.push({
    id: messageIdCounter++,
    text: message.text,
    sender: message.sender,
    timestamp: new Date(),
    team: message.team as "left" | "right",
    profileImage: profileImage,
  });

  if (testMessageIndex === testMessages.length - 1) {
    testMessageIndex = 0;
  } else {
    testMessageIndex++;
  }
};

// 테스트용 시청자 채팅 추가
const addTestAudienceChat = () => {
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

const addMessages = () => {
  // 개발용: 테스트 메시지 자동 추가 (실제 배포시에는 제거)
  setTimeout(() => {
    addTestMessage();
  }, 1000);

  setTimeout(() => {
    addTestMessage();
  }, 2000);

  setTimeout(() => {
    addTestMessage();
  }, 3000);

  setTimeout(() => {
    addTestMessage();
  }, 4000);

  setTimeout(() => {
    addTestMessage();
  }, 5000);

  setTimeout(() => {
    addTestMessage();
  }, 6000);

  // 시청자 채팅 메시지를 더 많이 추가
  for (let i = 0; i < 30; i++) {
    setTimeout(() => {
      addTestAudienceChat();
    }, 100 * i);
  }
};

// 토론 정보 영역 토글 함수
const toggleDebateInfo = () => {
  isDebateInfoCollapsed.value = !isDebateInfoCollapsed.value;
};

const subscribeToDebateRoom = () => {
  console.log("🔍 subscribeToDebateRoom");
  const stompClient = createStompClientWrapper(client.value);
  if (stompClient) {
    stompClient.subscribe(
      `/sub/debate/room/${roomId.value}/start/opinion`,
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
    stompClient.subscribe(
      `/sub/debate/room/${roomId.value}/speak/start`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSpeakStartMessage;
        console.log("🔍 진영논리 발언 시작: ", msg);
        isPreparationTime.value = false;
        currentSpeaker.value = msg.speaker;
        speakerStartAt.value = msg.speakerStartAt + "+09:00";
        currentStageIndex.value = 0;
        currentStage.value = stages[currentStageIndex.value];
        speakingDuration.value = 60 * 1000;
        startSpeakingTimer();
      }
    );
    stompClient.subscribe(
      `/sub/debate/room/${roomId.value}/speak/end`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSpeakEndMessage;
        console.log("🔍 진영논리 발언 종료: ", msg);
        speakerEndAt.value = msg.speakerEndAt + "+09:00";
        startSpeakingTransitionTimer();
      }
    );
    stompClient.subscribe(
      `/sub/debate/room/${roomId.value}/start/battle`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSelectTargetMessage;
        console.log("🔍 공격 선택 시작: ", msg);
        isSelectTarget.value = true;
        selectTargetStartAt.value = msg.battleStartAt + "+09:00";
        currentStageIndex.value = 2;
        startSelectTargetTimer();
      }
    );
    stompClient.subscribe(
      `/sub/debate/room/${roomId.value}/attack`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSelectTargetResponse;
        console.log("🔍 공격 선택 응답: ", msg);
        if (msg.attacker === debateStore.myEmail) {
          selectTargetConfirmed.value = true;
        }
        targetSelectionStore.applySelect(msg);
      }
    );
    stompClient.subscribe(
      `/sub/debate/room/${roomId.value}/speak/attackStart`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSpeakStartMessage;
        console.log("🔍 공격 발언 시작: ", msg);
        currentSpeaker.value = msg.speaker;
        speakerStartAt.value = msg.speakerStartAt + "+09:00";
        currentStageIndex.value = 2;
        currentStage.value = stages[currentStageIndex.value];
        speakingDuration.value = 30 * 1000;
        startSpeakingTimer();
      }
    );
    stompClient.subscribe(
      `/sub/debate/room/${roomId.value}/speak/attackEnd`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSpeakEndMessage;
        console.log("🔍 공격 발언 종료: ", msg);
        speakerEndAt.value = msg.speakerEndAt + "+09:00";
        startSpeakingTransitionTimer();
      }
    );
    stompClient.subscribe(
      `/sub/debate/room/${roomId.value}/speak/defenseStart`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSpeakStartMessage;
        console.log("🔍 방어 발언 시작: ", msg);
        currentSpeaker.value = msg.speaker;
        speakerStartAt.value = msg.speakerStartAt + "+09:00";
        currentStageIndex.value = 2;
        currentStage.value = stages[currentStageIndex.value];
        speakingDuration.value = 30 * 1000;
        startSpeakingTimer();
      }
    );
    stompClient.subscribe(
      `/sub/debate/room/${roomId.value}/speak/defenseEnd`,
      (message) => {
        const msg = JSON.parse(message.body) as DebateSpeakEndMessage;
        console.log("🔍 방어 발언 종료: ", msg);
        speakerEndAt.value = msg.speakerEndAt + "+09:00";
        startSpeakingTransitionTimer();
      }
    );
    stompClient.subscribe(
      `/sub/debate/${roomId.value}/vote/start`,
      (message) => {
        console.log("🔍 투표 시작 알림: ", message);
      }
    );
    stompClient.subscribe(`/sub/debate/${roomId.value}/vote/end`, (message) => {
      console.log("🔍 투표 종료 알림: ", message);
    });
  }
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
      body: JSON.stringify({ userEmail: debateStore.myEmail }),
    }
  );

  const data = await response.json();
  const authToken = data.token;

  return authToken;
};

// 컴포넌트 마운트 시 초기화
onMounted(async () => {
  console.log("🚀 토론방 초기화 시작");

  // 라우트 파라미터 기반 설정 (이미 설정되어 있으면 유지)
  if (!debateStore.roomId) {
    const rid = String(route.params.id || roomStore.room?.roomId || "");
    if (rid) debateStore.setRoomId(rid);
  }
  if (!debateStore.myEmail) {
    // 기본값: 현재 사용자 이메일만 설정, 팀/프로듀서는 기존 값 유지
    debateStore.setMyInfo(authStore.userEmail || "", debateStore.myTeam, debateStore.isProducer);
  }

  // 개발용 빠른 시작: 커스텀 방에서 온 경우에는 건너뜀
  setTimeout(() => {
    // 서버 이벤트로 해제되지 않았으면 자동 해제
    if (isPreparationTime.value) {
      stepDone.completed.resolve();
      isPreparationTime.value = false;
    }
  }, 1200);

  await subscribeToDebateRoom();
  await startWebRTCConnection(roomStore.room?.roomId ?? "404");
  console.log("🔍 토론방 준비 완료: ", Date.now());
});

// 컴포넌트 언마운트 시 STOMP 연결 해제 및 정리
onUnmounted(() => {
  // WebRTC 연결 해제
  disconnectWebRTC();

  // 토론 상태 초기화 (방을 나갈 때는 resetAll 사용)
  debateStore.resetDebateState();
});
</script>

<style scoped>
/* 발언 중인 참가자의 프로필 이미지 빛나는 효과 */
.speaking-glow {
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

.speaking-glow::before {
  content: "";
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  border-radius: 50%;
  background: linear-gradient(
    45deg,
    #ff6b6b,
    #4ecdc4,
    #45b7d1,
    #96ceb4,
    #ffeaa7,
    #dda0dd
  );
  background-size: 300% 300%;
  animation: glow-pulse 2s ease-in-out infinite;
  z-index: -1;
}

.speaking-glow::after {
  content: "";
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border-radius: 50%;
  background: linear-gradient(
    45deg,
    #ff6b6b,
    #4ecdc4,
    #45b7d1,
    #96ceb4,
    #ffeaa7,
    #dda0dd
  );
  background-size: 300% 300%;
  animation: glow-rotate 3s linear infinite;
  opacity: 0.8;
  z-index: -1;
  filter: blur(8px);
}

@keyframes glow-pulse {
  0%,
  100% {
    background-position: 0% 50%;
    transform: scale(1);
    opacity: 0.8;
  }
  50% {
    background-position: 100% 50%;
    transform: scale(1.05);
    opacity: 1;
  }
}

@keyframes glow-rotate {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 200% 50%;
  }
}
</style>
