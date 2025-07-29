<template>
  <div
    class="bg-gradient-to-br overflow-hidden relative"
    style="height: calc(100vh - 64px)"
  >
    <!-- 준비시간 카운트다운 오버레이 -->
    <div
      v-if="isPreparationTime"
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
        <div class="w-96 bg-gray-700 rounded-full h-3 mx-auto">
          <div
            class="bg-blue-500 h-3 rounded-full transition-all duration-1000 ease-linear"
            :style="{ width: `${(preparationTimeLeft / 30) * 100}%` }"
          ></div>
        </div>

        <!-- 양 진영 정보 미리보기 -->
        <div class="mt-12 w-full max-w-4xl mx-auto">
          <div
            class="grid grid-cols-3 items-center gap-8"
            style="grid-template-columns: 1fr auto 1fr"
          >
            <!-- 좌측 진영 -->
            <div class="text-center">
              <div class="text-xl font-bold text-debate-left mb-4">
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
                    class="text-sm text-white text-center max-w-full break-words"
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
              <div class="text-xl font-bold text-debate-right mb-4">
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
                    class="text-sm text-white text-center max-w-full break-words"
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

    <!-- 메인 레이아웃: 좌측 토론 영역 + 우측 채팅 고정 -->
    <div class="flex h-full">
      <!-- 좌측: 토론 내용 영역 -->
      <div class="flex flex-col min-w-0 p-6 pr-3 flex-1">
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
              class="grid grid-cols-1 lg:grid-cols-2 gap-6 transition-all duration-700 ease-in-out"
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
                      <Avatar :class="'w-[min(8vw,60px)] h-[min(8vw,60px)]'">
                        <AvatarImage
                          :src="participant.profileImage"
                          :alt="participant.name"
                        />
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
                      <Avatar :class="'w-[min(8vw,60px)] h-[min(8vw,60px)]'">
                        <AvatarImage
                          :src="participant.profileImage"
                          :alt="participant.name"
                        />
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
        <Card class="flex-1 flex flex-col min-h-0 overflow-hidden">
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
          <div class="flex items-center space-x-2 mx-4">
            <!-- STT 컨트롤 버튼 -->
            <Button
              v-if="sttSupported"
              @click="startSTT"
              :variant="isSTTRecognizing ? 'destructive' : 'default'"
              size="sm"
              class="flex items-center space-x-1"
            >
              <!-- 마이크 아이콘 -->
              <svg
                class="w-3 h-3"
                :class="isSTTRecognizing ? 'animate-pulse' : ''"
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path
                  fill-rule="evenodd"
                  d="M7 4a3 3 0 016 0v4a3 3 0 11-6 0V4zm4 10.93A7.001 7.001 0 0017 8a1 1 0 10-2 0A5 5 0 015 8a1 1 0 00-2 0 7.001 7.001 0 006 6.93V17H6a1 1 0 100 2h8a1 1 0 100-2h-3v-2.07z"
                  clip-rule="evenodd"
                />
              </svg>
              <span class="text-xs">
                {{ isSTTRecognizing ? "중지" : "시작" }}
              </span>
            </Button>
          </div>


          <CardContent class="flex-1 flex flex-col min-h-0 p-0">
            <!-- 메시지 영역 (스크롤 가능) -->
            <ScrollArea
              ref="messagesScrollArea"
              class="flex-1 rounded-lg p-4 min-h-0 scroll-smooth"
              type="always"
            >
              <div
                ref="messagesContainer"
                id="messages-container"
                class="space-y-6"
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
                    <Avatar :class="'w-[min(6vw,64px)] h-[min(6vw,64px)]'">
                      <AvatarImage
                        :src="message.profileImage"
                        :alt="message.sender"
                      />
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
                  <div>
                    토론이 시작되면 실시간 발언 내용이 여기에 표시됩니다.
                  </div>
                </div>
              </div>
            </ScrollArea>

            <!-- 현재 발언자 표시 및 STT 실시간 결과 -->
            <div
              v-if="currentSpeaker || currentInterimText"
              class="mt-4 mx-6 mb-6"
            >
              <!-- 현재 발언자 표시 -->
              <Card
                v-if="currentSpeaker"
                class="bg-blue-50 border-blue-200 mb-3"
              >
                <CardContent class="">
                  <div class="flex items-center space-x-2">
                    <div
                      class="w-2 h-2 bg-red-500 rounded-full animate-pulse"
                    ></div>
                    <span class="text-sm font-medium text-blue-800">
                      {{ currentSpeaker
                      }}{{
                        currentSpeaker.includes("인식")
                          ? ""
                          : "님이 발언 중입니다..."
                      }}
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
      <div class="flex-shrink-0 w-[640px] p-6 pl-3">
        <!-- 시청자 채팅창 -->
        <Card class="h-full flex flex-col">
          <!-- 시청자 채팅 헤더 -->
          <CardHeader
            class="flex flex-row items-center justify-between space-y-0"
          >
            <CardTitle class="text-lg text-slate-700">채팅창</CardTitle>
            <Badge variant="secondary" class="flex items-center space-x-1">
              <div class="w-2 h-2 bg-green-500 rounded-full"></div>
              <span class="text-xs">{{ audienceCount }}명</span>
            </Badge>
          </CardHeader>

          <CardContent class="flex-1 flex flex-col min-h-0 p-0">
            <!-- 시청자 채팅 메시지 영역 -->
            <ScrollArea
              ref="audienceChatScrollArea"
              class="flex-1 rounded-lg p-3 min-h-0"
              style="height: calc(100vh - 300px)"
              type="always"
            >
              <div ref="audienceChatContainer" class="space-y-3">
                <div
                  v-for="chatMessage in audienceMessages"
                  :key="chatMessage.id"
                  class="flex flex-col space-y-1"
                >
                  <!-- 닉네임과 시간 -->
                  <div class="flex items-center justify-between">
                    <Badge
                      variant="outline"
                      class="text-xs font-medium truncate max-w-20"
                      :title="chatMessage.nickname"
                    >
                      {{ chatMessage.nickname }}
                    </Badge>
                    <!-- <span class="text-xs text-slate-400">{{
                      formatTime(chatMessage.timestamp)
                    }}</span> -->
                  </div>

                  <!-- 메시지 -->
                  <Card class="bg-white shadow-sm p-0">
                    <CardContent class="p-2">
                      <div class="text-xs text-slate-700">
                        {{ chatMessage.text }}
                      </div>
                    </CardContent>
                  </Card>
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

            <!-- 채팅 입력 영역 (관리자용 또는 테스트용) -->
            <CardFooter class="pt-3">
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
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from "vue";
import vikingProfile from "@/assets/images/profile/viking.png";
import gladiatorProfile from "@/assets/images/profile/gladiator.png";
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

// 토론 주제
const debateSubject = ref("인간은 태생적으로 선하다?");
const debateLeftTeam = ref("선하다");
const debateRightTeam = ref("악하다");

// UI 상태 관리
const isDebateInfoCollapsed = ref(false);
const isAnimating = ref(false);

// 토론 단계 관련
const stages = [
  "준비시간",
  "진영논리",
  "1차 요약",
  "공방전",
  "투표",
  "토론결과",
];
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

// 진영별 남은 시간
const leftTeamTime = ref("05:30");
const rightTeamTime = ref("04:15");

// 웹소켓 관련 변수들
const socket = ref<WebSocket | null>(null);
const isConnected = ref(false);
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

// 현재 발언자 (STT 활성화 시)
const currentSpeaker = ref<string | null>(null);

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
    currentSpeaker.value = "음성 인식 중...";
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

    // 중간 결과 업데이트
    currentInterimText.value = interimTranscript;

    // 최종 결과가 있으면 메시지로 추가
    if (finalTranscript.trim()) {
      currentFinalText.value = finalTranscript;

      // 토론 메시지로 추가 (현재는 기본 발언자로 설정, 실제로는 마이크 권한이나 사용자 설정에 따라 결정)
      const currentUser = leftTeam.value[0]; // 임시로 좌측 첫 번째 참가자로 설정

      messages.value.push({
        id: messageIdCounter++,
        text: finalTranscript.trim(),
        sender: currentUser.name,
        timestamp: new Date(),
        team: "left", // 실제로는 현재 사용자의 진영으로 설정
        profileImage: currentUser.profileImage,
      });

      console.log("STT 최종 결과:", finalTranscript);
    }

    console.log("STT 중간 결과:", interimTranscript);
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

    // 아직 인식 중이면 자동 재시작 (연속 인식)
    if (isSTTRecognizing.value) {
      console.log("STT 자동 재시작");
      try {
        recognitionInstance.start();
      } catch (error) {
        console.error("STT 재시작 오류:", error);
        stopSTT();
      }
    } else {
      currentSpeaker.value = null;
      currentInterimText.value = "";
      currentFinalText.value = "";
    }
  };

  return recognitionInstance;
};

// STT 시작
const startSTT = () => {
  if (!sttSupported.value) {
    alert("이 브라우저는 음성 인식을 지원하지 않습니다.");
    return;
  }

  if (isSTTRecognizing.value) {
    stopSTT();
    return;
  }

  recognition.value = initSTT();
  if (!recognition.value) return;

  try {
    recognition.value.start();

    // 설정된 시간 후 자동 종료
    stopTimer.value = setTimeout(() => {
      stopSTT();
      console.log(`${STT_DURATION / 1000}초간 음성 인식이 완료되었습니다.`);
    }, STT_DURATION);
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

  currentSpeaker.value = null;
  currentInterimText.value = "";
  currentFinalText.value = "";

  console.log("STT 음성 인식을 중지했습니다.");
};

// 준비시간 타이머 시작
const startPreparationTimer = () => {
  preparationTimeLeft.value = 5;
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
      // 다음 토론 단계로 진행 (준비시간 -> 진영논리)
      if (currentStageIndex.value === 0) {
        currentStageIndex.value = 1;
        currentStage.value = stages[currentStageIndex.value];
        console.log("토론 단계 변경:", currentStage.value);
      }
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

// WebSocket 연결
const connectWebSocket = () => {
  try {
    // TODO: 실제 백엔드 WebSocket URL로 변경 필요
    socket.value = new WebSocket("ws://localhost:8080/ws/stt");

    socket.value.onopen = () => {
      console.log("STT WebSocket 연결됨");
      isConnected.value = true;
    };

    socket.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        console.log("STT 메시지 수신:", data);

        if (data.type === "speech") {
          // STT로 변환된 발언 내용
          const team = determineTeam(data.speaker);
          const profileImage =
            team === "left" ? vikingProfile : gladiatorProfile;

          messages.value.push({
            id: messageIdCounter++,
            text: data.text || data.message,
            sender: data.speaker || data.sender,
            timestamp: new Date(data.timestamp || Date.now()),
            team: team,
            profileImage: profileImage,
          });
        } else if (data.type === "speaker_change") {
          // 발언자 변경
          currentSpeaker.value = data.speaker;
        } else if (data.type === "speech_end") {
          // 발언 종료
          currentSpeaker.value = null;
        }
      } catch (error) {
        console.error("STT 메시지 파싱 오류:", error);
      }
    };

    socket.value.onclose = () => {
      console.log("STT WebSocket 연결 해제됨");
      isConnected.value = false;
      currentSpeaker.value = null;

      // 재연결 시도 (5초 후)
      setTimeout(() => {
        if (!isConnected.value) {
          connectWebSocket();
        }
      }, 5000);
    };

    socket.value.onerror = (error) => {
      console.error("STT WebSocket 오류:", error);
      isConnected.value = false;
      currentSpeaker.value = null;
    };
  } catch (error) {
    console.error("STT WebSocket 연결 실패:", error);
    isConnected.value = false;
  }
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
      text: "농섧이 고단신지 히니어 자미로 칼감애일지가. 스허어 개흡탐아 렁온읋돕랭마의 죠감에안 움당어가. 번게 닠죤 칸심바키지 러시가지 아기쟁에 딸뗀셤이도 뇽은 라밀을 손워싰이다 람알다은 지션손. 새 온밀곡 나은만이는 란픈 무어굘산을 간는데 륑뎌다 로카는어식에 데섬엔을 엄어성드를. 이번개는 옷닷즌과 자고무안고 랴숭는 리언의 짓이뎔게를 선리를 짐사앵딩 앤옹벜똔거닜데고 갈스는. 셔안칼뢘 친아헹마다가 서뎝해에, 즈베놀아를 격늡띠리어 니걶 영네헌 워엄 노남사아이가 솔밋 아조는. 었아고 으느가 헬스루나까지 로머효아가골이고 위하침임 손러쑤륍의. 니혼게 소호를 슨어이, 그먼에 마각이. 후더갠 아뎌마 기아버 치즈멕기바를, 지잠 니엇개앙고는 아비왜의 븠이토다. 듬누시는 내국망세더개의 맘디타워와 롱앙져청 내하브흐어지 어김아다. 킥러더든다 뽀잉은 히새슥먀버같이 겨융일젠, 를미잉에. 한봅논아면 괘마걸 부컿벨에구는 소헨볜다 굴욶껄은 간막부의 제애를 어메나는 트흔엍탕과 사서아딘. 마도럭두 줄니그드공을 본듷어즈므 거구앙이다 자걱댬코고 뽕뎌기기 시다솜오좍 주시이즹 저츠로. 졸뮝어 콘로모즉의 에횰손 가즈빘애돈 데마으온기 이패 야조하인지만 이악마오를 으가히챰고 라엉에게 잘의다. 온간던 소이즈들이 삮릍멩학의 가키사리의 하포아지가 하간지다 믈기불을 신달란마겐으로, 디히닥순은 둠뉴엘닐혀부터 느어하윙. 엡그 디꾸반도면 뭐긱상다가 손진랍므오 놌비둥솰그던이고, 닥쭈옵챈볌이나, 여놔자가 러늑대를 으골상이 이립움누가. 는견컥 마웅살고는 아급스넝의 이넌 호핳그완이. 뎌뗴앝론의 숴준거됸가지 날재가 늬천는 엘타선아로 리느어를과 잘자괴 우으음련딩애줌소가. 디요이이고 넌싥게이어호를 라민익치가, 넌터가, 시레우짜곤 라밉니다 차오근에서, 김서죤초아 텨가의 고스다즈다 치딘서윙. 오바의 츠추아링으로 끼래사농이다 재졀킨룔 오머주긴어 거우카도 어고에.",
      sender: leftTeam.value[0].name,
      team: "left",
    },
    {
      text: "쎤겄터워의 딤마를 효어고 밈존비는 그냐난으로 앱선은 인껴 아즈말겼 가라본뱡보. 사로대디도 사려펴팀넥샙도 쌩돌쌀삐고 힝웡베고가 미렷이뜨는. 울이소구에서 거세 댄상들릴어 랬트허므니칸는 반나만익하 허힌다 그송료요다 아흐슬콜워의 구호비저라. 잔디호디까지 싄티숨먼도 지학차자 으랸리이이 요벼쥬일다 쭈틱구좌곡껴다 외자안갚닐내구의 죠긱고, 고다머다, 미볼이져웧어 들팀단. 왐번언뽐침을 란으송뢴어, 궝린러여는 센익으라, 링끄쇄오라가 구며 셔할을 어자놘나고 윽부를. 빅여 시자도 자아헐란잘았 근쇠성이면 라인젼이를 근기곌라. 공즙은 볘룬의 풔오압니다 사두바압고 거긱우다. 각힘 피얘나며 도친일 킴류몬에 익홌싀륵어 억경꾸. 징인을 루흔곱안곺의 반으젼을 말츤올 태나는 겹사짣더를 자자회도 앤갔돠채게에 영하따이를 저다난힙과. 수새줜을 붕자찬을 뎌비직, 훕아 가몬기 능뵈싀빙뤼일 겐안아 죽성넜티의 계리애지라고, 델악아뱌 그긴깄금굴벙이다. 증컵캐가 냠다가 외낼감으를 친제샘산다 잔이밸이로, 겨슥임궈로 만녝이비는 세오대손을 아젠이 소딕삭눈인촣으니. 쏼익쬬농이 하아온 발으로써 억이는 우저넘고임습니다. 잴가지르는 문에앙으로 릿거빠소롤 아헤대듭은 타트당속이요 즈갱쑈좜다. 렌랠츠서를 더처에 겨안다고 니아춰나 오사긱뉘자 탈네햐지 냉그러에엔은 자조엔어 신아넴자던 드암다. 사둬굑모가 자돌셥능고 송애가 라앝배아아서 미일면, 아거 임볼 되더둘근고 저힌숮아에. 싈아오사로 탈니은바에 눙눙어는 배노테롸 저둑. 판사하를 잉쿙는데 먼하로 쪼굠아점 익신의 직머석마는. 그난은 온이돈던에 턱다더기다, 옴뎔 기아가, 스어다 엳삘허는 져잡마겨로 윽단다 뤽글우러가. 잏저가시게 즈애돠기를 가연자저맀, 다악던, 닌굴자 백난은어 로물아갠다 아기스련이 구느좡악라가. 져족세미일걶 안돈논흩는 움엑뉑두갱브 그아혀라 로자오탑서됴에 왼구카나고 어사장암간안다 렌티에기 두껴딤은 지겁에서 삽친달부터.",
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
    { text: "재미있는 토론이네요!", nickname: "시청자1" },
    { text: "좌측 의견이 더 설득력 있는 것 같아요", nickname: "시청자2" },
    { text: "우측도 좋은 포인트네요", nickname: "시청자3" },
    { text: "철학적인 주제라 어렵네요 ㅠㅠ", nickname: "시청자4" },
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

// 준비시간 타이머 상태 변화 감지
watch(
  () => isPreparationTime.value,
  () => {
    if (isPreparationTime.value) {
      startPreparationTimer();
    } else {
      stopPreparationTimer();
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

// 컴포넌트 마운트 시 WebSocket 연결 및 STT 초기화
onMounted(() => {
  connectWebSocket();

  // STT 지원 여부 확인
  checkSTTSupport();
  console.log("STT 지원 여부:", sttSupported.value);

  // 준비시간 타이머 시작
  startPreparationTimer();

  // resize 이벤트 리스너 추가
  window.addEventListener("resize", handleResize);

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
});

// 컴포넌트 언마운트 시 WebSocket 연결 해제 및 STT 정리
onUnmounted(() => {
  if (socket.value) {
    socket.value.close();
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
