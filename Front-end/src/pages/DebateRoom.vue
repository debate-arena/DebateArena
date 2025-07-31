<template>
  <div
    class="bg-gradient-to-br relative"
    :style="{ height: isPreparationTime ? 'calc(100vh - 64px)' : '' }"
  >
    <!-- WebRTC 연결 중 화면 -->
    <div
      v-if="webrtcState.isConnecting"
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
          {{ webrtcState.connectionStepText }}
        </div>

        <!-- 진행 단계 표시 -->
        <div class="flex justify-center items-center space-x-4 mb-8">
          <div
            v-for="(step, index) in [
              'router',
              'transport',
              'producer',
              'consumer',
            ]"
            :key="step"
            :class="[
              'w-4 h-4 rounded-full transition-all duration-500',
              getStepIndex(webrtcState.connectionStep) >= index
                ? 'bg-blue-500'
                : 'bg-gray-600',
            ]"
          ></div>
        </div>

        <!-- 참가자 연결 상태 -->
        <div class="mt-12 w-full max-w-4xl mx-auto">
          <div class="text-xl text-white mb-6">참가자 연결 상태</div>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-12">
            <div
              v-for="participant in webrtcState.participants"
              :key="participant.id"
              class="flex flex-col items-center space-y-2"
            >
              <div class="relative">
                <Avatar class="w-16 h-16">
                  <AvatarImage
                    :src="participant.profileImage"
                    :alt="participant.name"
                  />
                </Avatar>
                <!-- 연결 상태 표시 -->
                <div
                  :class="[
                    'absolute -bottom-1 -right-1 w-6 h-6 rounded-full border-2 border-white flex items-center justify-center',
                    participant.connected ? 'bg-green-500' : 'bg-gray-400',
                  ]"
                >
                  <div v-if="participant.connected" class="text-white text-xs">
                    ✓
                  </div>
                  <div
                    v-else
                    class="w-3 h-3 bg-white rounded-full animate-pulse"
                  ></div>
                </div>
              </div>
              <span
                class="text-sm text-white text-center font-bold"
                style="
                  line-height: 1.2;
                  white-space: pre-line;
                  min-height: 2.5rem;
                "
              >
                {{
                  participant.name.length > 8
                    ? participant.name.slice(0, 8) +
                      "\n" +
                      participant.name.slice(8)
                    : participant.name
                }}
              </span>
              <span
                :class="[
                  'text-xs px-2 py-1 rounded-full',
                  participant.connected
                    ? 'bg-green-500/20 text-green-300'
                    : 'bg-gray-500/20 text-gray-300',
                ]"
              >
                {{ participant.connected ? "연결됨" : "연결 중..." }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 준비시간 화면 -->
    <div
      v-else-if="isPreparationTime"
      class="absolute inset-0 z-50 bg-black bg-opacity-80 flex items-center justify-center"
    >
      <div class="text-center">
        <!-- 메인 카운트다운 숫자 -->5
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
            :style="{ width: `${(preparationTimeLeft / 30) * 100}%` }"
          ></div>
        </div>

        <!-- 토론 주제 -->

        <div class="text-4xl font-bold text-white mb-8">
          {{ debateSubject }}
        </div>

        <!-- 양 진영 정보 미리보기 -->
        <div class="mt-12 w-full max-w-4xl mx-auto">
          <div
            class="grid grid-cols-3 items-center gap-8"
            style="grid-template-columns: 1fr auto 1fr"
          >
            <!-- 좌측 진영 -->
            <div class="text-center">
              <div class="text-2xl font-bold text-debate-left mb-4">
                {{ debateLeftTeam }}
              </div>
              <div class="flex justify-center space-x-2">
                <div
                  v-for="participant in leftTeam"
                  :key="participant.id"
                  class="flex flex-col items-center space-y-2 w-32"
                >
                  <Avatar class="w-16 h-16">
                    <AvatarImage
                      :src="participant.profileImage"
                      :alt="participant.name"
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
                      participant.name.length > 8
                        ? participant.name.slice(0, 8) +
                          "\n" +
                          participant.name.slice(8)
                        : participant.name
                    }}
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
                  v-for="participant in rightTeam"
                  :key="participant.id"
                  class="flex flex-col items-center space-y-2 w-32"
                >
                  <Avatar class="w-16 h-16">
                    <AvatarImage
                      :src="participant.profileImage"
                      :alt="participant.name"
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
                      participant.name.length > 8
                        ? participant.name.slice(0, 8) +
                          "\n" +
                          participant.name.slice(8)
                        : participant.name
                    }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 메인 토론 화면 - 준비시간이 끝난 후에만 렌더링 -->
    <div v-else class="flex h-full" style="min-height: 600px">
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
            <CardHeader class="pb-6 transition-all duration-700 ease-in-out">
              <CardTitle class="text-3xl font-bold text-center">{{
                debateSubject
              }}</CardTitle>
            </CardHeader>

            <!-- 현재 토론 단계 -->
            <Card
              class="text-center mb-4 transition-all duration-700 ease-in-out p-4"
              style="transition-delay: 100ms"
            >
              <CardContent>
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
            </Card>

            <!-- 토론 정보 그리드 -->
            <div
              class="grid grid-cols-1 2xl:grid-cols-2 gap-6 transition-all duration-700 ease-in-out"
              style="transition-delay: 200ms"
            >
              <!-- 좌측 진영 정보 -->
              <Card
                class="bg-debate-left transition-all duration-700 ease-in-out border-debate-left text-black gap-2"
                style="transition-delay: 300ms"
              >
                <CardHeader>
                  <CardTitle class="text-4xl text-center">{{
                    debateLeftTeam
                  }}</CardTitle>
                </CardHeader>
                <CardContent class="space-y-2">
                  <div class="grid grid-cols-2 gap-2">
                    <div
                      v-for="participant in leftTeam"
                      :key="participant.id"
                      class="flex flex-col items-center space-y-1"
                    >
                      <Avatar
                        :class="[
                          'w-16 h-16 relative overflow-visible',
                          participant.id === debateSession.currentSpeakerId
                            ? 'speaking-glow'
                            : '',
                        ]"
                      >
                        <AvatarImage
                          :src="participant.profileImage"
                          :alt="participant.name"
                        />
                        <!-- 공격/수비 아이콘 -->
                        <div
                          v-if="participantStates[participant.id]"
                          class="absolute -top-8 z-10 w-16 h-16 flex items-center justify-center"
                        >
                          <img
                            :src="
                              participantStates[participant.id] === 'attack'
                                ? attackIcon
                                : defenseIcon
                            "
                            :alt="
                              participantStates[participant.id] === 'attack'
                                ? '공격'
                                : '수비'
                            "
                            class="w-8 h-8"
                          />
                        </div>
                      </Avatar>
                      <span
                        class="text-xs sm:text-sm font-medium text-center max-w-full break-words"
                        style="
                          word-break: keep-all;
                          overflow-wrap: break-word;
                          line-height: 1.2;
                          white-space: pre-line;
                        "
                      >
                        {{
                          participant.name.length > 8
                            ? participant.name.slice(0, 8) +
                              "\n" +
                              participant.name.slice(8)
                            : participant.name
                        }}
                      </span>
                    </div>
                  </div>
                  <div class="mt-3 text-center">
                    <div class="text-xs">남은 시간</div>
                    <div class="text-lg font-bold">{{ leftTeamTime }}</div>
                  </div>
                </CardContent>
              </Card>

              <!-- 우측 진영 정보 -->
              <Card
                class="bg-debate-right transition-all duration-700 ease-in-out border-debate-right text-white gap-2"
                style="transition-delay: 300ms"
              >
                <CardHeader class="p-0">
                  <CardTitle class="text-4xl text-center">{{
                    debateRightTeam
                  }}</CardTitle>
                </CardHeader>
                <CardContent class="space-y-2">
                  <div class="grid grid-cols-2 gap-2">
                    <div
                      v-for="participant in rightTeam"
                      :key="participant.id"
                      class="flex flex-col items-center space-y-1"
                    >
                      <Avatar
                        :class="[
                          'w-16 h-16 relative overflow-visible',
                          participant.id === debateSession.currentSpeakerId
                            ? 'speaking-glow'
                            : '',
                        ]"
                      >
                        <AvatarImage
                          :src="participant.profileImage"
                          :alt="participant.name"
                        />
                        <!-- 공격/수비 아이콘 -->
                        <div
                          v-if="participantStates[participant.id]"
                          class="absolute -top-8 z-10 w-16 h-16 flex items-center justify-center"
                        >
                          <img
                            :src="
                              participantStates[participant.id] === 'attack'
                                ? attackIcon
                                : defenseIcon
                            "
                            :alt="
                              participantStates[participant.id] === 'attack'
                                ? '공격'
                                : '수비'
                            "
                            class="w-8 h-8"
                          />
                        </div>
                      </Avatar>
                      <span
                        class="text-xs sm:text-sm font-medium text-center max-w-full break-words"
                        style="
                          word-break: keep-all;
                          overflow-wrap: break-word;
                          line-height: 1.2;
                          white-space: pre-line;
                        "
                      >
                        {{
                          participant.name.length > 8
                            ? participant.name.slice(0, 8) +
                              "\n" +
                              participant.name.slice(8)
                            : participant.name
                        }}
                      </span>
                    </div>
                  </div>
                  <div class="mt-3 text-center">
                    <div class="text-xs">남은 시간</div>
                    <div class="text-lg font-bold">{{ rightTeamTime }}</div>
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
          <!-- 채팅 헤더 -->
          <CardHeader
            class="flex flex-row items-center justify-between space-y-0"
          >
            <CardTitle class="text-xl">실시간 토론 내용</CardTitle>
            <Badge
              :variant="isConnected ? 'default' : 'destructive'"
              class="flex items-center space-x-1"
            >
              <div
                :class="[
                  'w-2 h-2 rounded-full',
                  isConnected ? 'bg-green-500' : 'bg-red-500',
                ]"
              ></div>
              <span class="text-xs">
                {{ isConnected ? "WS 연결됨" : "WS 연결 끊김" }}
              </span>
            </Badge>
          </CardHeader>

          <!-- 현재 발언자 및 대기시간 표시 -->
          <div class="mx-4 mb-4">
            <Card
              v-if="currentSpeakerInfo || debateSession.stage === 'transition'"
              class="mb-4"
              :class="{
                'bg-blue-50 border-blue-200':
                  debateSession.stage === 'speaking',
                'bg-yellow-50 border-yellow-200':
                  debateSession.stage === 'transition',
              }"
            >
              <CardContent class="py-3">
                <!-- 현재 발언 중 -->
                <div
                  v-if="
                    debateSession.stage === 'speaking' && currentSpeakerInfo
                  "
                  class="flex items-center justify-between"
                >
                  <div class="flex items-center space-x-3">
                    <Avatar class="w-10 h-10">
                      <AvatarImage :src="currentSpeakerInfo.profileImage" />
                    </Avatar>
                    <div>
                      <div class="font-medium">
                        {{ currentSpeakerInfo.name }}
                      </div>
                      <div class="text-sm text-blue-600">현재 발언 중</div>
                    </div>
                  </div>
                  <div class="text-right">
                    <div class="text-2xl font-bold text-blue-600">
                      {{ Math.floor(debateSession.remainingTime / 60) }}:{{
                        String(debateSession.remainingTime % 60).padStart(
                          2,
                          "0"
                        )
                      }}
                    </div>
                    <div class="text-xs text-gray-500">남은 시간</div>
                  </div>
                </div>

                <!-- 대기시간 표시 -->
                <div
                  v-else-if="debateSession.stage === 'transition'"
                  class="flex items-center justify-center space-x-4"
                >
                  <div class="text-center">
                    <div class="text-3xl font-bold text-yellow-600 mb-2">
                      {{ debateSession.transitionTimeLeft }}
                    </div>
                    <div class="text-sm text-yellow-700">
                      다음 발언자 준비 중...
                    </div>
                    <div
                      v-if="isNextSpeaker"
                      class="text-xs text-yellow-800 mt-1 font-medium"
                    >
                      곧 당신 차례입니다!
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

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
                      :src="message.profileImage"
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

            <!-- 현재 발언자 표시 및 STT 실시간 결과 -->
            <div
              v-if="currentSpeakerInfo || currentInterimText"
              class="mt-4 mx-6 mb-6"
            >
              <!-- 현재 발언자 표시 -->
              <Card
                v-if="currentSpeakerInfo"
                class="bg-blue-50 border-blue-200 mb-3"
              >
                <CardContent class="">
                  <div class="flex items-center space-x-2">
                    <div
                      class="w-2 h-2 bg-red-500 rounded-full animate-pulse"
                    ></div>
                    <span class="text-sm font-medium text-blue-800">
                      {{ currentSpeakerInfo.name }}님이 발언 중입니다...
                    </span>
                  </div>
                </CardContent>
              </Card>

              <!-- STT 실시간 중간 결과 표시 -->
              <Card
                v-if="currentInterimText"
                class="bg-yellow-50 border-yellow-200"
              >
                <CardContent class="pt-3">
                  <div class="flex flex-col space-y-2">
                    <div class="flex items-center justify-center space-x-2">
                      <div
                        class="w-2 h-2 bg-yellow-500 rounded-full animate-pulse"
                      ></div>
                      <span class="text-xs font-medium text-yellow-700">
                        실시간 음성 인식 중...
                      </span>
                    </div>
                    <div
                      class="text-sm text-gray-700 bg-white p-2 rounded border-l-4 border-yellow-400"
                    >
                      {{ currentInterimText }}
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </CardContent>
        </Card>
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
import { ref, onMounted, onUnmounted, nextTick, watch, computed } from "vue";
import vikingProfile from "@/assets/images/profile/viking.png";
import gladiatorProfile from "@/assets/images/profile/gladiator.png";
import audienceProfile from "@/assets/images/profile/audience.png";
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
import { useDebateConnection } from "@/composables/useDebateConnection";
import { useAuthStore } from "@/store/auth";
import type {
  Speaker,
  DebateSession,
  SpeakerChangeMessage,
  STTMessage,
  SessionUpdateMessage,
} from "@/types/debate";

const debateSession = ref<DebateSession>({
  currentSpeakerId: null,
  nextSpeakerId: null,
  speakingTimeLimit: 60,
  remainingTime: 60,
  speakingOrder: [1, 2, 3, 4], // 발언 순서
  currentOrderIndex: 0,
  stage: "waiting",
  transitionTimeLeft: 0,
});

//STOMP 연결 관련 
const authStore = useAuthStore();
const { 
  client, 
  isConnected, 
  isConnecting,
  connectionError,
  isTestMode,
  getCurrentUser,
  startConnection, 
  stopConnection: disconnect,
  joinRoom,
  leaveRoom,
  sendSignalingMessage,
  sendDebateMessage,
  // WebRTC 관련
  participants,
  isProducerTransportReady,
  isMediasoupLoaded,
  localVideoRef,
  startProduce
} = useDebateConnection();

// 현재 사용자 ID (테스트 모드 지원)
const currentUserId = ref(isTestMode ? 1 : (authStore.user?.id ? parseInt(authStore.user.id) : 1));

// 발언권 확인
const canSpeak = computed(() => {
  return (
    debateSession.value.currentSpeakerId === currentUserId.value &&
    debateSession.value.stage === "speaking"
  );
});

// WebRTC 상태 호환성을 위한 computed
const webrtcState = computed(() => ({
  isConnecting: isConnecting.value,
  isConnected: isConnected.value,
  connectionStep: isProducerTransportReady.value ? 'completed' : 'transport',
  connectionStepText: isConnecting.value 
    ? 'WebRTC 연결 중...' 
    : isProducerTransportReady.value 
      ? '연결 완료!' 
      : 'MediaSoup 초기화 중...',
  participants: participants.value.map(p => ({
    id: p.producerUserEmail, // 이메일을 ID로 사용
    name: p.producerUserEmail.split('@')[0], // 이메일에서 이름 추출
    email: p.producerUserEmail,
    profileImage: '/default-avatar.png',
    connected: p.videoStream !== null
  }))
}));

// 모든 참가자 연결 상태
const allParticipantsConnected = computed(() => {
  return participants.value.length > 0 && 
         participants.value.every(p => p.videoStream !== null);
});

// WebRTC 연결 시작 (호환성)
const startWebRTCConnection = async (participantsList: any[]) => {
  console.log('🚀 WebRTC 연결 시작 (새로운 구현)');
  
  // 기존 연결이 없다면 STOMP 연결부터 시작
  if (!isConnected.value) {
    await startConnection();
  }
  
  // 토론방 입장
  const roomId = "test-room"; // 실제로는 라우터에서 받아와야 함
  joinRoom(roomId);
  
  return true;
};

// WebRTC 연결 해제 (호환성)
const disconnectWebRTC = () => {
  console.log('🔌 WebRTC 연결 해제 (새로운 구현)');
  leaveRoom();
};
// 다음 발언자인지 확인
const isNextSpeaker = computed(() => {
  return debateSession.value.nextSpeakerId === currentUserId.value;
});

// 현재 발언자 정보
const currentSpeakerInfo = computed(() => {
  const speakerId = debateSession.value.currentSpeakerId;
  if (!speakerId) return null;

  const allParticipants = [...leftTeam.value, ...rightTeam.value];
  return allParticipants.find((p) => p.id === speakerId);
});

// STOMP 메시지 핸들러들
const handleSpeakerChange = (data: SpeakerChangeMessage) => {
  debateSession.value.currentSpeakerId = data.currentSpeakerId;
  debateSession.value.nextSpeakerId = data.nextSpeakerId;
  debateSession.value.remainingTime = data.remainingTime;

  if (data.transitionTimeLeft !== undefined) {
    debateSession.value.transitionTimeLeft = data.transitionTimeLeft;
    debateSession.value.stage = "transition";
    startTransitionTimer();
  } else if (data.currentSpeakerId) {
    debateSession.value.stage = "speaking";
  } else {
    debateSession.value.stage = "waiting";
  }

  // 자동으로 STT 중지 (발언권이 없어진 경우)
  if (!canSpeak.value && isSTTRecognizing.value) {
    stopSTT();
  }
};

const handleSTTResult = (data: STTMessage) => {
  // 서버에서 오는 STT 결과를 메시지로 추가
  const speakerInfo = [...leftTeam.value, ...rightTeam.value].find(
    (p) => p.id === data.speakerId
  );

  if (speakerInfo) {
    if (!data.isInterim) {
      // 최종 결과만 메시지로 추가
      messages.value.push({
        id: messageIdCounter++,
        text: data.text,
        sender: speakerInfo.name,
        timestamp: new Date(data.timestamp),
        team: speakerInfo.id <= 2 ? "left" : "right", // 임시 로직
        profileImage: speakerInfo.profileImage,
      });
    } else {
      // 중간 결과는 실시간 표시용
      currentInterimText.value = data.text;
    }
  }
};

const handleSessionUpdate = (data: SessionUpdateMessage) => {
  debateSession.value.stage = data.stage;
  debateSession.value.currentOrderIndex = data.currentOrderIndex;
  debateSession.value.speakingOrder = data.speakingOrder;
};

// 3초 대기시간 타이머
const transitionTimer = ref<number | null>(null);

const startTransitionTimer = () => {
  if (transitionTimer.value) {
    clearInterval(transitionTimer.value);
  }

  transitionTimer.value = setInterval(() => {
    debateSession.value.transitionTimeLeft--;
    if (debateSession.value.transitionTimeLeft <= 0) {
      clearInterval(transitionTimer.value!);
      transitionTimer.value = null;
      // 대기시간 종료 후 서버에서 자동으로 다음 발언자로 변경됨
    }
  }, 1000);
};

// STT 버튼 텍스트 결정 함수 제거됨 (자동 제어로 변경)

// STOMP 구독 설정
const roomId = "test-room"; // 실제로는 라우터에서 가져와야 함

const subscribeToDebateRoom = () => {
  if (!client.value || !isConnected.value) {
    console.warn('STOMP 클라이언트가 연결되지 않음');
    return;
  }

  console.log('🔔 토론방 구독 시작...');

  try {
    // 발언 순서 변경 구독
    client.value.subscribe(`/topic/debate/${roomId}/speaker`, (message) => {
      const data = JSON.parse(message.body) as SpeakerChangeMessage;
      console.log('👤 발언자 변경:', data);
      handleSpeakerChange(data);
    });

    // STT 텍스트 실시간 수신
    client.value.subscribe(`/topic/debate/${roomId}/stt`, (message) => {
      const data = JSON.parse(message.body) as STTMessage;
      console.log('🎤 STT 결과 수신:', data);
      handleSTTResult(data);
    });

    // 토론 세션 상태 변경
    client.value.subscribe(`/topic/debate/${roomId}/session`, (message) => {
      const data = JSON.parse(message.body) as SessionUpdateMessage;
      console.log('📊 세션 상태 변경:', data);
      handleSessionUpdate(data);
    });

    console.log('✅ 토론방 구독 완료');
  } catch (error) {
    console.error('❌ 토론방 구독 실패:', error);
  }
};

// STT 결과를 서버로 전송
const sendSTTResult = (text: string, isInterim: boolean) => {
  if (!isConnected.value) {
    console.warn('STOMP 연결이 없음 - STT 결과 전송 실패');
    return;
  }

  if (!canSpeak.value) {
    console.warn('발언권이 없음 - STT 결과 전송 건너뛰기');
    return;
  }

  try {
    const sttData = {
      speakerId: currentUserId.value,
      text,
      isInterim,
      timestamp: Date.now(),
    };

    if (client.value) {
      client.value.publish({
        destination: `/app/debate/${roomId}/stt`,
        body: JSON.stringify(sttData),
        headers: {
          'content-type': 'application/json'
        }
      });
      
      console.log('🎤 STT 결과 전송:', { text: text.substring(0, 50), isInterim });
    }
  } catch (error) {
    console.error('❌ STT 결과 전송 실패:', error);
  }
};

// STT 자동 시작 (서버에서 발언권 제어)
const startSTT = () => {
  if (!sttSupported.value) {
    console.warn("이 브라우저는 음성 인식을 지원하지 않습니다.");
    return;
  }

  // 이미 인식 중이면 먼저 중지
  if (isSTTRecognizing.value) {
    stopSTT();
  }

  recognition.value = initSTT();
  if (!recognition.value) return;

  try {
    recognition.value.start();

    // 서버에 발언 시작 알림
    if (client.value) {
      client.value.publish({
        destination: `/app/debate/${roomId}/speaking/start`,
        body: JSON.stringify({
          speakerId: currentUserId.value,
          timestamp: Date.now(),
        }),
      });
    }
  } catch (error) {
    console.error("STT 시작 오류:", error);
    isSTTRecognizing.value = false;
  }
};

// STT 중지
const stopSTT = () => {
  if (recognition.value) {
    isSTTRecognizing.value = false;
    recognition.value.stop();
  }

  if (stopTimer.value) {
    clearTimeout(stopTimer.value);
    stopTimer.value = null;
  }

  currentInterimText.value = "";
  currentFinalText.value = "";

  // 서버에 발언 종료 알림
  if (client.value && canSpeak.value) {
    client.value.publish({
      destination: `/app/debate/${roomId}/speaking/end`,
      body: JSON.stringify({
        speakerId: currentUserId.value,
        timestamp: Date.now(),
      }),
    });
  }

  console.log("STT 음성 인식을 중지했습니다.");
};

// STT 지원 여부 확인
const checkSTTSupport = () => {
  const SpeechRecognition =
    (window as any).SpeechRecognition ||
    (window as any).webkitSpeechRecognition;
  sttSupported.value = !!SpeechRecognition;
  return SpeechRecognition;
};

// STT 초기화
const initSTT = () => {
  const SpeechRecognition = checkSTTSupport();
  if (!SpeechRecognition) {
    console.warn("이 브라우저는 Speech Recognition을 지원하지 않습니다.");
    return null;
  }

  const recognitionInstance = new SpeechRecognition();
  recognitionInstance.continuous = true;
  recognitionInstance.interimResults = true;
  recognitionInstance.lang = "ko-KR";
  recognitionInstance.maxAlternatives = 1;

  // 음성 인식 시작 이벤트
  recognitionInstance.onstart = () => {
    isSTTRecognizing.value = true;
    console.log("STT 음성 인식을 시작합니다.");
  };

  // 음성 인식 결과 이벤트
  recognitionInstance.onresult = (event: any) => {
    let finalTranscript = "";
    let interimTranscript = "";

    for (let i = event.resultIndex; i < event.results.length; ++i) {
      const transcript = event.results[i][0].transcript;
      if (event.results[i].isFinal) {
        finalTranscript += transcript;
      } else {
        interimTranscript += transcript;
      }
    }

    // 중간 결과는 실시간으로 서버에 전송
    if (interimTranscript.trim()) {
      sendSTTResult(interimTranscript.trim(), true);
      currentInterimText.value = interimTranscript;
    }

    // 최종 결과도 서버에 전송
    if (finalTranscript.trim()) {
      sendSTTResult(finalTranscript.trim(), false);
      currentInterimText.value = "";
    }
  };

  // 음성 인식 오류 이벤트
  recognitionInstance.onerror = (event: any) => {
    console.error("STT 오류:", event.error);
    if (event.error === "not-allowed") {
      alert(
        "마이크 권한이 필요합니다. 브라우저 설정에서 마이크 권한을 허용해주세요."
      );
    }
  };

  // 음성 인식 종료 이벤트
  recognitionInstance.onend = () => {
    console.log("STT 음성 인식이 종료되었습니다.");

    if (isSTTRecognizing.value) {
      console.log("STT 자동 재시작");
      try {
        recognitionInstance.start();
      } catch (error) {
        console.error("STT 재시작 오류:", error);
        stopSTT();
      }
    } else {
      currentInterimText.value = "";
      currentFinalText.value = "";
    }
  };

  return recognitionInstance;
};

// 토론 주제
const debateSubject = ref("인간은 태생적으로 선하다?");
const debateLeftTeam = ref("선하다");
const debateRightTeam = ref("악하다");

// UI 상태 관리
const isDebateInfoCollapsed = ref(false);
const isAnimating = ref(false);

// 토론 단계 관련
const stages = ["진영논리", "1차 요약", "공방전", "투표", "토론결과"];
const currentStageIndex = ref(0);
const currentStage = ref(stages[currentStageIndex.value]);

// 진영별 참여자 정보
const leftTeam = ref([
  { id: 1, name: "암솝니다중넎랑이돈고지온곺은", profileImage: vikingProfile },
  {
    id: 2,
    name: "여덟글자채우려면여덟글자채우려면",
    profileImage: vikingProfile,
  },
]);

const rightTeam = ref([
  { id: 3, name: "Lorem잉에늬다ipsum", profileImage: gladiatorProfile },
  { id: 4, name: "정지원", profileImage: gladiatorProfile },
]);

// 참가자별 공격/수비 상태 관리
const participantStates = ref<Record<number, "attack" | "defense" | null>>({
  1: "attack", // 테스트용: 첫 번째 참가자는 공격 상태
  2: "defense", // 테스트용: 두 번째 참가자는 수비 상태
  3: "attack", // 테스트용: 세 번째 참가자는 공격 상태
  4: null, // 네 번째 참가자는 아이콘 표시 안함
});

// 진영별 남은 시간
const leftTeamTime = ref("05:30");
const rightTeamTime = ref("04:15");

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
const preparationTimer = ref<number | null>(null);
const PREPARATION_DURATION = 30 * 1000; // 30초

// 준비시간 타이머 시작
const startPreparationTimer = () => {
  preparationTimeLeft.value = 3;
  isPreparationTime.value = true;
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
      isPreparationTime.value = false;
      // 준비시간 종료 후 토론 시작 로직 추가
      console.log("준비시간 종료. 토론 시작!");
    }
  }, 1000);
};

// 준비시간 타이머 중지
const stopPreparationTimer = () => {
  if (preparationTimer.value) {
    clearInterval(preparationTimer.value);
    preparationTimer.value = null;
  }
};

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

// ScrollArea 강제 업데이트 함수
const forceUpdateScrollArea = () => {
  nextTick(() => {
    // 메시지 ScrollArea 업데이트
    if (messagesScrollArea.value) {
      updateScrollAreaViewport(messagesScrollArea.value);
    }

    // 시청자 채팅 ScrollArea 업데이트
    if (audienceChatScrollArea.value) {
      updateScrollAreaViewport(audienceChatScrollArea.value);
    }
  });
};

// ScrollArea viewport 업데이트 헬퍼 함수
const updateScrollAreaViewport = (scrollAreaRef: any) => {
  if (!scrollAreaRef) return;

  let viewport = null;

  // 방법 1: data-slot 속성으로 찾기
  viewport = scrollAreaRef.$el?.querySelector(
    '[data-slot="scroll-area-viewport"]'
  );

  // 방법 2: class로 찾기 (backup)
  if (!viewport) {
    viewport = scrollAreaRef.$el?.querySelector(".size-full");
  }

  // 방법 3: 직접 scrollArea 엘리먼트에서 스크롤 시도
  if (!viewport) {
    viewport = scrollAreaRef.$el;
  }

  if (viewport) {
    // 스크롤 영역 강제 업데이트를 위해 스크롤 이벤트 트리거
    const scrollTop = viewport.scrollTop;
    viewport.scrollTop = scrollTop + 1;
    viewport.scrollTop = scrollTop;

    // resize 이벤트 트리거
    if (window.ResizeObserver) {
      const resizeEvent = new Event("resize");
      window.dispatchEvent(resizeEvent);
    }
  }
};

// UI 토글 함수
const toggleDebateInfo = () => {
  isAnimating.value = true;
  isDebateInfoCollapsed.value = !isDebateInfoCollapsed.value;
  setTimeout(() => {
    isAnimating.value = false;
    // 애니메이션 완료 후 스크롤바 업데이트
    forceUpdateScrollArea();
  }, 500);
};

// WebRTC 연결 단계 인덱스 반환
const getStepIndex = (step: string): number => {
  const steps = ["router", "transport", "producer", "consumer", "completed"];
  return steps.indexOf(step);
};

// 발언자 이름으로 진영 판단 (실제로는 백엔드에서 진영 정보를 함께 보내야 함)
const determineTeam = (speaker: string): "left" | "right" => {
  const leftTeamMembers = leftTeam.value.map((member) => member.name);
  const rightTeamMembers = rightTeam.value.map((member) => member.name);

  if (leftTeamMembers.includes(speaker)) {
    return "left";
  } else if (rightTeamMembers.includes(speaker)) {
    return "right";
  }

  // 기본값은 좌측 진영 (실제로는 더 정확한 로직 필요)
  return "left";
};

// 발언자 이름으로 참가자 ID 찾기
const findParticipantIdByName = (speaker: string): number | null => {
  const allParticipants = [...leftTeam.value, ...rightTeam.value];
  const participant = allParticipants.find((p) => p.name === speaker);
  return participant ? participant.id : null;
};

// 참가자 상태 변경 함수들
const setParticipantState = (
  participantId: number,
  state: "attack" | "defense" | null
) => {
  participantStates.value[participantId] = state;
};

const setParticipantAttack = (participantId: number) => {
  participantStates.value[participantId] = "attack";
};

const setParticipantDefense = (participantId: number) => {
  participantStates.value[participantId] = "defense";
};

const clearParticipantState = (participantId: number) => {
  participantStates.value[participantId] = null;
};

// 모든 참가자 상태 초기화
const clearAllParticipantStates = () => {
  Object.keys(participantStates.value).forEach((id) => {
    participantStates.value[Number(id)] = null;
  });
};

// 채팅 스크롤을 맨 아래로 이동
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesScrollArea.value) {
      // 여러 가지 방법으로 viewport를 찾아보기
      let viewport = null;

      // 방법 1: data-slot 속성으로 찾기
      viewport = messagesScrollArea.value.$el?.querySelector(
        '[data-slot="scroll-area-viewport"]'
      );

      // 방법 2: class로 찾기 (backup)
      if (!viewport) {
        viewport = messagesScrollArea.value.$el?.querySelector(".size-full");
      }

      // 방법 3: 직접 scrollArea 엘리먼트에서 스크롤 시도
      if (!viewport) {
        viewport = messagesScrollArea.value.$el;
      }

      if (viewport) {
        console.log(
          "Scrolling to bottom, scrollHeight:",
          viewport.scrollHeight
        );
        viewport.scrollTop = viewport.scrollHeight;
      } else {
        console.log("Viewport not found");
      }
    }
  });
};

// 시청자 채팅 스크롤을 맨 아래로 이동
const scrollAudienceChatToBottom = () => {
  nextTick(() => {
    if (audienceChatScrollArea.value) {
      // 여러 가지 방법으로 viewport를 찾아보기
      let viewport = null;

      // 방법 1: data-slot 속성으로 찾기
      viewport = audienceChatScrollArea.value.$el?.querySelector(
        '[data-slot="scroll-area-viewport"]'
      );

      // 방법 2: class로 찾기 (backup)
      if (!viewport) {
        viewport =
          audienceChatScrollArea.value.$el?.querySelector(".size-full");
      }

      // 방법 3: 직접 scrollArea 엘리먼트에서 스크롤 시도
      if (!viewport) {
        viewport = audienceChatScrollArea.value.$el;
      }

      if (viewport) {
        console.log(
          "Scrolling audience chat to bottom, scrollHeight:",
          viewport.scrollHeight
        );
        viewport.scrollTop = viewport.scrollHeight;
      } else {
        console.log("Audience viewport not found");
      }
    }
  });
};

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
      nickname: "관리자", // 실제로는 사용자 닉네임
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
      sender: leftTeam.value[0].name,
      team: "left",
    },
    {
      text: "그 사장은 시작은 여성이 이제는 않은 네 어둡어서 나로 호조에 입다. 난다 자르면 체포는, 인식을 태웁니다 하류로 없이 하나에 냉장고 전해지게 갈다. 말을 것 측 시간이 들리다. 8가지 우리를 그 높은 모기업으로 낮다, 어느 살벌하다. 죽자면 본 재생으로 나아 안 지방이 한식집에 독특하다. 지도로 시대를 느껴지어 부처를 주는 돈이고 즐기다. 우리가 이어서 마구 구필에 상대적, 도입은 허둥거린 검사받은 조성되어야지. 없이 있은 참조는 가지는 회사에 시각화한 신규도 통과도 말하여서 말하다. 앵무새를 앞을 된 혼합이 혈에 게임에, 된다 움츠러들다. 수 쇠고기다 그래서 뜨다, 같는 그러나 변화되더니 본다 그다음이 기름이다 엷다. 반점은 말면 언젠가 보급은 많다나 낸다. 무엇을 아이고 벌이며 세력의 모든 성장시키다 보게, 숨차아 말미암아요. 생산의 회복은 낸 말을 산 진입하여서 진짜, 결과로 갖아 훌륭하다. 줄넘기를 볼 번개의 나에 결의하게 거기에 것 위하다. 것 집에서 보는데 하여간 탐독한 줄까 국산화하고 지난해와 포기하여요. 소금보다 시비에 또는 혜안도, 없다.",
      sender: rightTeam.value[0].name,
      team: "right",
    },
  ];

  const message = testMessages[testMessageIndex];
  const profileImage =
    message.team === "left" ? vikingProfile : gladiatorProfile;

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

// 메시지가 추가될 때마다 자동으로 스크롤
watch(
  () => messages.value.length,
  () => {
    // 대안 방법: 마지막 메시지로 스크롤
    nextTick(() => {
      const lastMessage = messagesContainer.value?.lastElementChild;
      if (lastMessage) {
        lastMessage.scrollIntoView({ behavior: "smooth", block: "end" });
      } else {
        scrollToBottom();
      }
    });
  },
  { flush: "post" }
);

// 시청자 채팅이 추가될 때마다 자동으로 스크롤
watch(
  () => audienceMessages.value.length,
  () => {
    // 대안 방법: 마지막 채팅으로 스크롤
    nextTick(() => {
      const lastChat = audienceChatContainer.value?.lastElementChild;
      if (lastChat) {
        lastChat.scrollIntoView({ behavior: "smooth", block: "end" });
      } else {
        scrollAudienceChatToBottom();
      }
    });
  },
  { flush: "post" }
);

// 토론 정보 확장/축소 상태 변화 감지
watch(
  () => isDebateInfoCollapsed.value,
  () => {
    // 애니메이션이 완료될 때까지 대기 후 스크롤바 업데이트
    setTimeout(() => {
      forceUpdateScrollArea();
    }, 550); // 애니메이션 시간(500ms)보다 약간 늦게
  },
  { flush: "post" }
);

// 준비시간 타이머 상태 변화 감지 (WebRTC 연결과 관계없이 수동 제어)
watch(
  () => isPreparationTime.value,
  () => {
    if (isPreparationTime.value) {
      startPreparationTimer();
    } else {
      stopPreparationTimer();
    }
  },
  { immediate: false } // 수동으로 제어하므로 즉시 실행하지 않음
);

// 발언권 변경 시 STT 자동 시작/중지
watch(
  () => canSpeak.value,
  (newVal) => {
    if (newVal) {
      startSTT();
    } else {
      stopSTT();
    }
  },
  { immediate: true } // 컴포넌트 마운트 시 즉시 실행
);

// resize 타이머 변수
let resizeTimer: number | null = null;

// resize 이벤트 핸들러
const handleResize = () => {
  // 디바운스를 위해 타이머 사용
  if (resizeTimer) {
    clearTimeout(resizeTimer);
  }
  resizeTimer = setTimeout(() => {
    forceUpdateScrollArea();
  }, 150);
};

// WebRTC 연결 시작
const initializeWebRTCConnection = async () => {
  try {
    console.log("🚀 WebRTC 연결 시작...");

    // 참가자 정보 (실제로는 서버에서 받아와야 함)
    const participants = [
      {
        id: 1,
        name: leftTeam.value[0].name,
        email: "user1@example.com",
        profileImage: leftTeam.value[0].profileImage,
      },
      {
        id: 2,
        name: leftTeam.value[1].name,
        email: "user2@example.com",
        profileImage: leftTeam.value[1].profileImage,
      },
      {
        id: 3,
        name: rightTeam.value[0].name,
        email: "user3@example.com",
        profileImage: rightTeam.value[0].profileImage,
      },
      {
        id: 4,
        name: rightTeam.value[1].name,
        email: "user4@example.com",
        profileImage: rightTeam.value[1].profileImage,
      },
    ];

    await startWebRTCConnection(participants);
    console.log("✅ WebRTC 연결 완료, 준비시간 시작");

    // WebRTC 연결 완료 후 준비시간 시작
    isPreparationTime.value = true;
    startPreparationTimer();
  } catch (error) {
    console.error("❌ WebRTC 연결 실패:", error);
    // WebRTC 연결에 실패해도 토론은 계속 진행 (fallback)
    console.log("📢 WebRTC 없이 토론 진행");
    isPreparationTime.value = true;
    startPreparationTimer();
  }
};

// 컴포넌트 마운트 시 초기화
onMounted(async () => {
  console.log('🚀 토론방 초기화 시작');
  
  // 테스트 모드 정보 출력
  if (isTestMode) {
    const testUser = getCurrentUser();
    console.log('🧪 테스트 모드 활성화');
    console.log('👤 테스트 사용자:', testUser);
  }
  
  try {
    // 1. 인증 상태 확인 (테스트 모드에서는 우회)
    if (!isTestMode && !authStore.isLoggedIn) {
      console.error('❌ 로그인이 필요합니다.');
      return;
    }

    // 2. STOMP 연결 시작 (useDebateConnection에서 자동으로 처리됨)
    console.log('🔌 STOMP 연결 시작...');
    const connected = await startConnection();
    
    if (!connected) {
      console.error('❌ STOMP 연결 실패:', connectionError.value);
      return;
    }

    console.log('✅ STOMP 연결 성공');

    // 2.1 시그널링 시작
    console.log('🔌 시그널링 연결 시작...');
    joinRoom(roomId);


    // 3. 토론방 구독
    subscribeToDebateRoom();
    console.log('✅ 토론방 구독 완료');

  } catch (error) {
    console.error('💥 토론방 초기화 실패:', error);
  }

  // 4. STT 지원 여부 확인
  checkSTTSupport();
  console.log("STT 지원 여부:", sttSupported.value);

  // 5. WebRTC 연결 시작 (STOMP 연결 후 시작)
  initializeWebRTCConnection();

  // 6. UI 이벤트 리스너 추가
  window.addEventListener("resize", handleResize);

  // 7. 개발용 테스트 메시지 (배포시 제거)
  setTimeout(() => {
    addMessages();
  }, 10000);
});

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

// 컴포넌트 언마운트 시 STOMP 연결 해제 및 정리
onUnmounted(() => {
  // STOMP 연결 해제
  disconnect();

  // WebRTC 연결 해제
  disconnectWebRTC();

  // 타이머들 정리
  if (transitionTimer.value) {
    clearInterval(transitionTimer.value);
  }

  // STT 정리
  stopSTT();
  stopPreparationTimer(); // 준비시간 타이머 정리

  // resize 이벤트 리스너 제거
  window.removeEventListener("resize", handleResize);

  // 타이머 정리
  if (resizeTimer) {
    clearTimeout(resizeTimer);
    resizeTimer = null;
  }
});
</script>

<style scoped>
/* 발언 중인 참가자의 프로필 이미지 빛나는 효과 */
.speaking-glow {
  position: relative;
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
