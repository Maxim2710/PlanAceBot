package com.PlanAceBot.service;

import com.PlanAceBot.model.Reminder;
import com.PlanAceBot.model.Task;
import com.PlanAceBot.model.User;
import com.PlanAceBot.state.*;
import com.PlanAceBot.config.BotConfig;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private static final String HELP_TEXT = """
            :information_source: Список доступных команд:

            /start - Регистрация пользователя и приветственное сообщение.
            /create_task - Создание новой задачи.
            /update_task - Обновление существующей задачи.
            /delete_task - Удаление задачи.
            /change_status_task - Смена существующей задачи.
            /help - Показать инструкцию по командам.
            /list_tasks - Показать все задачи пользователя.
            /set_deadline_task - Установить дедлайн для задачи.
            /create_reminder - Создание нового напоминания.
            /update_reminder - Обновление существующего напоминания.
            /delete_reminder - Удаление напоминания.
            /list_reminders - Показать все напоминания пользователя.
            /show_task_commands - Отобразить все команды для взаимодействия с задачами.
            

            """;

    private static final String COMMAND_START = "/start";
    private static final String COMMAND_CREATE = "/create_task";
    private static final String COMMAND_UPDATE = "/update_task";
    private static final String COMMAND_DELETE = "/delete_task";
    private static final String COMMAND_CHANGE_STATUS = "/change_status_task";
    private static final String COMMAND_HELP = "/help";
    private static final String COMMAND_LIST_TASKS = "/list_tasks";
    private static final String COMMAND_SET_DEADLINE = "/set_deadline_task";
    private static final String COMMAND_CREATE_REMINDER = "/create_reminder";
    private static final String COMMAND_UPDATE_REMINDER = "/update_reminder";
    private static final String COMMAND_DELETE_REMINDER = "/delete_reminder";
    private static final String COMMAND_LIST_REMINDERS = "/list_reminders";
    private static final String COMMAND_SHOW_TASK_COMMANDS = "/show_task_commands";

    private static final String BUTTON_TITLE = "Название";
    private static final String BUTTON_DESCRIPTION = "Описание";
    private static final String BUTTON_PRIORITY = "Приоритет";
    private static final String BUTTON_CANCEL = "Отмена";
    private static final String BUTTON_CONFIRM = "Да";
    private static final String BUTTON_CANCEL_UPDATE = "Нет";
    private static final String BUTTON_SUBSCRIBE = "Подписаться";
    private static final String BUTTON_CHECK_SUBSCRIPTION = "Проверить подписку";
    private static final String BUTTON_REMIND_AT = "Время напоминания";
    private static final String BUTTON_MESSAGE = "Сообщение";
    private static final String CHANNEL_NAME = "development_max";
    private static final String CHANNEL_USERNAME = "@development_max";

    private final Map<String, TaskCreationState> taskCreationStates = new HashMap<>();
    private final Map<String, TaskUpdateState> taskUpdateStates = new HashMap<>();
    private final Map<String, List<Integer>> taskDeletionStates = new HashMap<>();
    private final Map<String, Integer> taskDeadlineStates = new HashMap<>();
    private final Map<String, ReminderCreationState> reminderCreationStates = new HashMap<>();
    private final Map<String, Integer> reminderCustomTimeStates = new ConcurrentHashMap<>();
    private final Map<String, ReminderUpdateState> reminderUpdateStates = new HashMap<>();
    private final Map<String, List<Long>> reminderDeletionStates = new HashMap<>();

    @Autowired
    private UserService userService;

    @Autowired
    private BotConfig botConfig;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ReminderService reminderService;

    public TelegramBot(BotConfig config) {
        this.botConfig = config;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "Регистрация пользователя и приветственное сообщение"));
        listofCommands.add(new BotCommand("/create_task", "Создание новой задачи"));
        listofCommands.add(new BotCommand("/update_task", "Обновление существующей задачи"));
        listofCommands.add(new BotCommand("/delete_task", "Удаление задачи"));
        listofCommands.add(new BotCommand("/change_status_task", "Смена статуса задачи"));
        listofCommands.add(new BotCommand("/help", "Показать инструкцию по командам"));
        listofCommands.add(new BotCommand("/list_tasks", "Показать все задачи пользователя"));
        listofCommands.add(new BotCommand("/set_deadline_task", "Установить дедлайн для задачи"));
        listofCommands.add(new BotCommand("/create_reminder", "Создание нового напоминания"));
        listofCommands.add(new BotCommand("/update_reminder", "Обновление существующего напоминания"));
        listofCommands.add(new BotCommand("/delete_reminder", "Удаление напоминания"));
        listofCommands.add(new BotCommand("/list_reminders", "Показать все напоминания пользователя"));
        listofCommands.add(new BotCommand("/show_task_commands", "Отобразить все команды для взаимодействия с задачами"));

        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessageUpdate(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void handleMessageUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            String[] parts = messageText.split(" ", 2);
            String command = parts[0];

            if (!userService.existByChatId(Long.parseLong(chatId))) {
                if (!command.equals(COMMAND_START)) {
                    sendSubscribeMessage(chatId);
                    return;
                }
            }

            if (!isUserSubscribed(chatId)) {
                sendSubscribeMessage(chatId);
                return;
            }

            command = switch (messageText) {
                case "\uD83D\uDCDD Создать задачу" -> COMMAND_CREATE;
                case "\uD83D\uDD8A Обновить задачу" -> COMMAND_UPDATE;
                case "\uD83D\uDDD1 Удалить задачу" -> COMMAND_DELETE;
                case "\uD83D\uDCDB Изменить статус" -> COMMAND_CHANGE_STATUS;
                case "\uD83D\uDCCB Список задач" -> COMMAND_LIST_TASKS;
                case "\u23F0 Установить дедлайн" -> COMMAND_SET_DEADLINE;
                case "\uD83D\uDCCB Задачи" -> COMMAND_SHOW_TASK_COMMANDS;
                case "◀ Вернуться назад" -> COMMAND_START;
                default -> messageText.split(" ", 2)[0];
            };

            if (taskCreationStates.containsKey(chatId)) {
                processTaskCreation(chatId, messageText);
            } else if (taskUpdateStates.containsKey(chatId)) {
                processFieldAndValue(chatId, messageText);
            } else if (taskDeletionStates.containsKey(chatId)) {
                sendDeleteConfirmationMessage(chatId, taskDeletionStates.get(chatId).get(0));
            } else if (taskDeadlineStates.containsKey(chatId)) {
                processDeadlineInput(chatId, messageText);
            } else if (reminderCreationStates.containsKey(chatId)) {
                processReminderCreation(chatId, messageText);
            } else if (reminderCustomTimeStates.containsKey(chatId)) {
                processCustomTimeInput(chatId, messageText);
            } else if (reminderUpdateStates.containsKey(chatId)) {
                processFieldAndValueForReminder(chatId, messageText);
            } else if (reminderDeletionStates.containsKey(chatId)) {
                sendDeleteReminderConfirmationMessage(chatId, reminderDeletionStates.get(chatId).get(0));
            } else {
                switch (command) {
                    case COMMAND_START:
                        registerUserAndSendWelcomeMessage(chatId, !messageText.equals("◀ Вернуться назад"));
                        break;

                    case COMMAND_CREATE:
                        handleTaskCreationCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_UPDATE:
                        handleUpdateCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_DELETE:
                        handleDeleteCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_CHANGE_STATUS:
                        handleChangeStatusCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_HELP:
                        sendHelpMessage(chatId, parts);
                        break;

                    case COMMAND_LIST_TASKS:
                        handleListTasksCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_SET_DEADLINE:
                        handleSetDeadlineCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_CREATE_REMINDER:
                        handleReminderCreationCommand(chatId);
                        break;

                    case COMMAND_UPDATE_REMINDER:
                        handleUpdateReminderCommand(parts, chatId);
                        break;

                    case COMMAND_DELETE_REMINDER:
                        handleDeleteReminderCommand(parts, chatId);
                        break;

                    case COMMAND_LIST_REMINDERS:
                        handleListRemindersCommand(chatId);
                        break;

                    case COMMAND_SHOW_TASK_COMMANDS:
                        showCommandsKeyboard(chatId);
                        break;

                    default:
                        sendUnknownCommandMessage(chatId);
                        break;
                }
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void showCommandsKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createKeyboardRow("\uD83D\uDCDD Создать задачу", "\uD83D\uDD8A Обновить задачу", "\uD83D\uDDD1 Удалить задачу"));
        keyboard.add(createKeyboardRow("\uD83D\uDCDB Изменить статус", "\uD83D\uDCCB Список задач"));
        keyboard.add(createKeyboardRow("\u23F0 Установить дедлайн"));
        keyboard.add(createKeyboardRow("◀ Вернуться назад"));

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите команду:")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending commands keyboard: {}", e.getMessage());
        }
    }

    private KeyboardRow createKeyboardRow(String... buttons) {
        KeyboardRow row = new KeyboardRow();
        for (String button : buttons) {
            row.add(button);
        }
        return row;
    }

    private void sendHelpMessage(String chatId, String[] parts) {
        if (parts.length > 1) {
            sendMessage(chatId, "Неверный формат команды. Используйте команду /help только без параметров.");
            return;
        }

        String helpMessage = EmojiParser.parseToUnicode(HELP_TEXT);
        sendMessage(chatId, helpMessage);
    }

    private void sendSubscribeMessage(String chatId) {
        String subscribeMessage = "Подпишитесь на наш канал и затем нажмите кнопку \"Проверить подписку\", чтобы продолжить использование бота.";
        InlineKeyboardMarkup markup = createSubscribeMarkup();
        SendMessage message = createMessage(chatId, subscribeMessage, markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending subscribe message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createSubscribeMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        InlineKeyboardButton subscribeButton = new InlineKeyboardButton();
        subscribeButton.setText(BUTTON_SUBSCRIBE);
        subscribeButton.setUrl("https://t.me/" + CHANNEL_NAME);
        row1.add(subscribeButton);

        InlineKeyboardButton checkSubscriptionButton = new InlineKeyboardButton();
        checkSubscriptionButton.setText(BUTTON_CHECK_SUBSCRIPTION);
        checkSubscriptionButton.setCallbackData("check_subscription");
        row2.add(checkSubscriptionButton);

        keyboard.add(row1);
        keyboard.add(row2);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private boolean isUserSubscribed(String chatId) {
        Long chatIdLong = Long.parseLong(chatId);

        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(CHANNEL_USERNAME);
        getChatMember.setUserId(chatIdLong);

        try {
            ChatMember chatMember = execute(getChatMember);
            String status = chatMember.getStatus();
            return !"left".equals(status) && !"kicked".equals(status);
        } catch (TelegramApiException e) {
            log.error("Error checking subscription status: {}", e.getMessage());
            return false;
        }
    }

    private void registerUserAndSendWelcomeMessage(String chatId, boolean flag) {
        boolean isNewUser = false;

        if (!userService.existByChatId(Long.parseLong(chatId))) {
            User currentUser = new User();
            currentUser.setChatId(Long.parseLong(chatId));
            currentUser.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userService.save(currentUser);
            isNewUser = true;
        }

        if (isNewUser && flag) {
            sendWelcomeMessage(chatId);
        } else if (!isNewUser && flag) {
            sendWelcomeBackMessage(chatId);
        } else {
            createStartKeyboardForBack(chatId, "Вы вернулись в главное меню");
        }
    }


    private void sendWelcomeMessage(String chatId) {
        String welcomeMessage = EmojiParser.parseToUnicode("Добро пожаловать! Я бот для управления задачами. :blush:\n" +
                "Используйте команду /help, чтобы увидеть список доступных команд.");
        sendMessage(chatId, welcomeMessage);

        createStartKeyboardForWelcome(chatId, welcomeMessage);
    }

    private void sendWelcomeBackMessage(String chatId) {
        String welcomeBackMessage = EmojiParser.parseToUnicode("С возвращением! :blush:\n" +
                "Используйте команду /help, чтобы увидеть список доступных команд.");

        createStartKeyboardForWelcomeBack(chatId, welcomeBackMessage);
    }

    private void createStartKeyboardForBack(String chatId, String backMessage) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("\uD83D\uDCCB Задачи");
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(backMessage)
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending welcome message: {}", e.getMessage());
        }
    }

    private void createStartKeyboardForWelcome(String chatId, String welcomeMessage) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("\uD83D\uDCCB Задачи");
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(welcomeMessage)
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending welcome message: {}", e.getMessage());
        }
    }

    private void createStartKeyboardForWelcomeBack(String chatId, String welcomeBackMessage) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("\uD83D\uDCCB Задачи");
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(welcomeBackMessage)
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending welcome message: {}", e.getMessage());
        }
    }

    private void handleTaskCreationCommand(String chatId, String[] parts, String messageText) {
        if (parts.length == 1 || messageText.equals("\uD83D\uDCDD Создать задачу")) {
            startTaskCreation(chatId);
        } else {
            sendMessage(chatId, "Неверный формат команды. Используйте /create_task без параметров.");
        }
    }

    private void startTaskCreation(String chatId) {
        if (getUserTaskCount(chatId) >= 20) {
            sendMessage(chatId, "Вы достигли максимального количества задач (20). Удалите существующие задачи перед созданием новых.");
            return;
        }

        taskCreationStates.put(chatId, new TaskCreationState());
        sendMessage(chatId, "Введите название задачи:");
    }

    private void processTaskCreation(String chatId, String messageText) {
        TaskCreationState currentState = taskCreationStates.get(chatId);

        if (currentState.getState() == TaskState.ENTER_TITLE) {
            currentState.setTitle(messageText);
            currentState.setState(TaskState.ENTER_DESCRIPTION);
            sendMessage(chatId, "Введите описание задачи для '" + messageText + "':");
        } else if (currentState.getState() == TaskState.ENTER_DESCRIPTION) {
            currentState.setDescription(messageText);
            currentState.setState(TaskState.ENTER_PRIORITY);
            sendMessage(chatId, "Введите приоритет задачи (1-5):");
        } else if (currentState.getState() == TaskState.ENTER_PRIORITY) {
            try {
                int priority = Integer.parseInt(messageText);
                if (priority < 1 || priority > 5) {
                    sendMessage(chatId, "Приоритет должен быть в диапазоне от 1 до 5. Пожалуйста, введите заново:");
                    return;
                }
                currentState.setPriority(priority);

                createTask(currentState.getTitle(), currentState.getDescription(), currentState.getPriority(), chatId);
                taskCreationStates.remove(chatId);

                sendMessage(chatId, "Задача '" + currentState.getTitle() + "' с приоритетом " + priority + " создана.");
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Пожалуйста, введите числовое значение для приоритета (1-5):");
            }
        }
    }

    private void createTask(String title, String description, int priority, String chatId) {
        if (getUserTaskCount(chatId) >= 20) {
            sendMessage(chatId, "Вы достигли максимального количества задач (20). Удалите существующие задачи перед созданием новых.");
            return;
        }

        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "Пользователь не зарегистрирован. Используйте /start для регистрации.");
            return;
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setUser(user);
        task.setCompleted(false);
        task.setCreationTimestamp(Timestamp.from(Instant.now()));
        task.setPriority(priority);

        taskService.save(task);
    }

    private int getUserTaskCount(String chatId) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            log.error("User not found for chatId: {}", chatId);
            return 0;
        }

        return taskService.countTasksByUser(user);
    }

    private void sendUnknownCommandMessage(String chatId) {
        sendMessage(chatId, "Неизвестная команда. Используйте /help, чтобы увидеть доступные команды.");
    }

    private void handleUpdateCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals("\uD83D\uDD8A Обновить задачу"))) {
            sendMessage(chatId, "Неверный формат команды. Используйте команду /update_task только без параметров.");
            return;
        }

        List<Task> tasks = taskService.findTasksByUserId(Long.parseLong(chatId));

        if (tasks.isEmpty()) {
            sendMessage(chatId, "У вас пока нет задач для обновления.");
            return;
        }

        InlineKeyboardMarkup markup = createTasksMarkup(tasks);

        SendMessage message = createMessage(chatId, "Выберите задачу для обновления:", markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending task selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createTasksMarkup(List<Task> tasks) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Task task : tasks) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(task.getTitle());
            button.setCallbackData("update_task_" + task.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void sendFieldSelectionMessage(String chatId) {
        TaskUpdateState currentState = taskUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "Ошибка обновления задачи.");
            return;
        }

        int taskId = currentState.getTaskId();

        Task task = taskService.findById(taskId);
        if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "Задача с указанным номером не найдена или не принадлежит вам.");
            return;
        }

        String currentTitle = task.getTitle();
        String currentDescription = task.getDescription();
        int currentPriority = task.getPriority();
        LocalDateTime creationTimestamp = task.getCreationTimestamp().toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String selectionMessage = "Выберите, что вы хотите обновить для задачи:\n";
        selectionMessage += "Текущее название: " + currentTitle + "\n";
        selectionMessage += "Текущее описание: " + currentDescription + "\n";
        selectionMessage += "Текущий приоритет: " + currentPriority + "\n";
        selectionMessage += "Дата создания: " + creationTimestamp.format(formatter) + "\n";

        LocalDateTime deadline = task.getDeadline();
        if (deadline != null) {
            selectionMessage += "Дедлайн: " + deadline.format(formatter) + "\n";
        }

        InlineKeyboardMarkup markup = createUpdateMarkup();

        SendMessage message = createMessage(chatId, selectionMessage, markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending field selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createUpdateMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton(BUTTON_TITLE, "update_title"));
        row1.add(createInlineButton(BUTTON_DESCRIPTION, "update_description"));
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton(BUTTON_PRIORITY, "update_priority"));
        row2.add(createInlineButton(BUTTON_CANCEL, "update_cancel"));
        keyboard.add(row2);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private List<InlineKeyboardButton> createButtonRow(String buttonText, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(buttonText);
        button.setCallbackData(callbackData);
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        return row;
    }

    private SendMessage createMessage(String chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(markup);
        return message;
    }

    private void processFieldAndValue(String chatId, String messageText) {
        TaskUpdateState currentState = taskUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "Ошибка при обновлении задачи.");
            return;
        }

        int taskId = currentState.getTaskId();
        String fieldToUpdate = currentState.getFieldToUpdate();

        Task task = taskService.findById(taskId);
        if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "Задача с указанным номером не найдена или не принадлежит вам.");
            taskUpdateStates.remove(chatId);
            return;
        }

        if (fieldToUpdate.equals("priority")) {
            try {
                int priority = Integer.parseInt(messageText);
                if (priority < 1 || priority > 5) {
                    sendMessage(chatId, "Приоритет должен быть в диапазоне от 1 до 5. Пожалуйста, введите заново:");
                    return;
                }
                task.setPriority(priority);
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Пожалуйста, введите числовое значение для приоритета (1-5):");
                return;
            }
        } else {
            updateTaskField(task, fieldToUpdate, messageText);
        }

        taskService.save(task);

        sendConfirmationMessage(chatId, task);
    }

    private void updateTaskField(Task task, String fieldToUpdate, String newValue) {
        switch (fieldToUpdate) {
            case "title":
                task.setTitle(newValue);
                break;
            case "description":
                task.setDescription(newValue);
                break;
            case "priority":
                task.setPriority(Integer.parseInt(newValue));
                break;
            default:
                sendMessage(task.getUser().getChatId().toString(), "Ошибка при обновлении задачи.");
                taskUpdateStates.remove(task.getUser().getChatId().toString());
                return;
        }

        taskService.save(task);
    }

    private void sendConfirmationMessage(String chatId, Task task) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("Изменения сохранены:\n");
        confirmationMessage.append("Название: ").append(task.getTitle()).append("\n");
        confirmationMessage.append("Описание: ").append(task.getDescription()).append("\n");
        confirmationMessage.append("Приоритет: ").append(task.getPriority()).append("\n");

        confirmationMessage.append("\nДата создания: ").append(task.getCreationTimestamp().toLocalDateTime().format(formatter));

        LocalDateTime deadline = task.getDeadline();
        if (deadline != null) {
            confirmationMessage.append("\nДедлайн: ").append(deadline.format(formatter));
        }

        confirmationMessage.append("\n\nПодтвердить изменения?");

        InlineKeyboardMarkup markup = createConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow(BUTTON_CONFIRM, "confirm_update");
        row.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_update"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private InlineKeyboardButton createInlineButton(String buttonText, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(buttonText);
        button.setCallbackData(callbackData);
        return button;
    }

    private void cancelUpdate(String chatId, TaskUpdateState currentState) {
        taskService.save(currentState.getOriginalTask());
        taskUpdateStates.remove(chatId);
        sendMessage(chatId, "Изменения отменены.");
    }

    private void sendNewValueRequest(String chatId, String field) {
        String messageText;
        switch (field) {
            case "title":
                messageText = "Введите новое название задачи:";
                break;
            case "description":
                messageText = "Введите новое описание задачи:";
                break;
            case "priority":
                messageText = "Введите новый приоритет задачи (1-5):";
                break;
            default:
                log.error("Unsupported field type: {}", field);
                return;
        }

        sendMessage(chatId, messageText);
    }

    public void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: {}", e.getMessage());
        }
    }

    private void editMessageReplyMarkup(String chatId, Integer messageId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        markup.setKeyboard(keyboard);

        EditMessageReplyMarkup editMessage = new EditMessageReplyMarkup();
        editMessage.setChatId(chatId);
        editMessage.setMessageId(messageId);
        editMessage.setReplyMarkup(markup);

        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            log.error("Error editing message reply markup: {}", e.getMessage());
        }
    }

    private void handleDeleteCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals("\uD83D\uDDD1 Удалить задачу"))) {
            sendMessage(chatId, "Неверный формат команды. Используйте /delete_task без параметров.");
            return;
        }

        List<Task> tasks = taskService.findTasksByUserId(Long.parseLong(chatId));

        if (tasks.isEmpty()) {
            sendMessage(chatId, "У вас нет задач для удаления.");
            return;
        }

        InlineKeyboardMarkup markup = createDeleteTaskMarkup(tasks);

        SendMessage message = createMessage(chatId, "Выберите задачу для удаления:", markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending delete task selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createDeleteTaskMarkup(List<Task> tasks) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Task task : tasks) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(task.getTitle());
            button.setCallbackData("delete_task_" + task.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void sendDeleteConfirmationMessage(String chatId, int taskIdToDelete) {
        Task task = taskService.findById(taskIdToDelete);
        if (task == null) {
            sendMessage(chatId, "Задача не найдена.");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("Вы уверены, что хотите удалить следующую задачу?\n\n");
        confirmationMessage.append("Название: ").append(task.getTitle()).append("\n");
        confirmationMessage.append("Описание: ").append(task.getDescription()).append("\n");
        confirmationMessage.append("Приоритет: ").append(task.getPriority()).append("\n");
        confirmationMessage.append("Дата создания: ").append(task.getCreationTimestamp().toLocalDateTime().format(formatter)).append("\n");

        LocalDateTime deadline = task.getDeadline();
        if (deadline != null) {
            confirmationMessage.append("Дедлайн: ").append(deadline.format(formatter)).append("\n");
        }

        InlineKeyboardMarkup markup = createDeleteConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending delete confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createDeleteConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow(BUTTON_CONFIRM, "confirm_delete");
        row.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_delete"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void confirmDelete(String chatId, List<Integer> taskIdsToDelete) {
        for (Integer taskId : taskIdsToDelete) {
            taskService.delete(taskId);
        }
        taskDeletionStates.remove(chatId);
        sendMessage(chatId, "Задача удалена.");
    }

    private void cancelDelete(String chatId) {
        taskDeletionStates.remove(chatId);
        sendMessage(chatId, "Удаление отменено.");
    }

    private void handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String chatId = callbackQuery.getMessage().getChatId().toString();
        String data = callbackQuery.getData();

        if ("check_subscription".equals(data)) {
            if (isUserSubscribed(chatId)) {
                registerUserAndSendWelcomeMessage(chatId, true);
            } else {
                sendMessage(chatId, "Вы еще не подписались на канал. Пожалуйста, подпишитесь и нажмите \"Проверить подписку\".");
            }
        } else if (data.startsWith("reschedule_")) {
            handleReschedule(data, chatId);
        }  else if ("confirm_yes".equals(data)) {
            ReminderCreationState currentState = reminderCreationStates.get(chatId);
            if (currentState != null) {
                createReminder(currentState.getMessage(), currentState.getReminderTime(), chatId);
                reminderCreationStates.remove(chatId);

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String formattedReminderTime = formatter.format(currentState.getReminderTime());

                sendMessage(chatId, "Напоминание установлено на " + formattedReminderTime + ".");
            }
        } else if (data.startsWith("deleteOk_")) {
            int reminderId = Integer.parseInt(data.split("_")[1]);
            deleteReminder(chatId, reminderId);
        } else if ("confirm_no".equals(data)) {
            reminderCreationStates.remove(chatId);
            sendMessage(chatId, "Создание напоминания отменено.");
        } else if (data.startsWith("update_task_")) {
            handleUpdateTask(data, chatId);
        } else if (data.startsWith("delete_task_")) {
            handleDeleteTask(data, chatId);
        } else if (data.startsWith("change_status_")) {
            handleChangeStatus(data, chatId);
        } else if (data.startsWith("status_completed_") || data.startsWith("status_cancel_change_")) {
            handleStatusChange(data, chatId);
        } else if (data.startsWith("set_deadline_")) {
            handleSetDeadlineTask(data, chatId);
        } else if (data.startsWith("update_reminder_")) {
            handleUpdateReminder(data, chatId);
        } else if (data.startsWith("delete_reminder_")) {
            handleDeleteReminder(data, chatId);
        } else {
            handleOtherStates(data, chatId);
        }

        editMessageReplyMarkup(chatId, callbackQuery.getMessage().getMessageId());
    }


    private void handleReschedule(String data, String chatId) {
        String[] parts = data.split("_");
        String action = parts[1];
        int reminderId = Integer.parseInt(parts[2]);

        switch (action) {
            case "5m":
                rescheduleReminder(chatId, reminderId, Duration.ofMinutes(5));
                break;
            case "1h":
                rescheduleReminder(chatId, reminderId, Duration.ofHours(1));
                break;
            case "1d":
                rescheduleReminder(chatId, reminderId, Duration.ofDays(1));
                break;
            case "custom":
                askForCustomTime(chatId, reminderId);
                break;
            default:
                sendMessage(chatId, "Неверная команда.");
                break;
        }
    }

    private void handleUpdateReminder(String data, String chatId) {
        String reminderIdString = data.substring("update_reminder_".length());
        try {
            int reminderId = Integer.parseInt(reminderIdString);
            Optional<Reminder> optionalReminder = reminderService.findReminderById(reminderId);

            if (optionalReminder.isEmpty()) {
                sendMessage(chatId, "Напоминание с указанным номером не найдено.");
                return;
            }

            Reminder reminder = optionalReminder.get();

            if (!reminder.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "Напоминание не принадлежит вам.");
                return;
            }

            reminderUpdateStates.put(chatId, new ReminderUpdateState((long) reminderId, "", reminder));
            sendFieldSelectionMessageForReminder(chatId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Ошибка при выборе напоминания для обновления.");
        }
    }


    private void handleSetDeadlineTask(String data, String chatId) {
        String taskIdString = data.substring("set_deadline_".length());
        try {
            int taskId = Integer.parseInt(taskIdString);
            Task task = taskService.findById(taskId);
            if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "Задача с указанным номером не найдена или не принадлежит вам.");
                return;
            }
            taskDeadlineStates.put(chatId, taskId);
            sendMessage(chatId, "Введите дедлайн для задачи в формате ГГГГ-ММ-ДД ЧЧ:ММ.");
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Ошибка при выборе задачи.");
        }
    }

    private void handleUpdateTask(String data, String chatId) {
        String taskIdString = data.substring("update_task_".length());
        try {
            int taskId = Integer.parseInt(taskIdString);
            Task task = taskService.findById(taskId);
            if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "Задача с указанным номером не найдена или не принадлежит вам.");
                return;
            }
            taskUpdateStates.put(chatId, new TaskUpdateState(taskId, "", task));
            sendFieldSelectionMessage(chatId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Ошибка при выборе задачи для обновления.");
        }
    }

    private void handleDeleteTask(String data, String chatId) {
        String taskIdString = data.substring("delete_task_".length());
        try {
            int taskId = Integer.parseInt(taskIdString);
            Task task = taskService.findById(taskId);
            if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "Задача с указанным номером не найдена или не принадлежит вам.");
                return;
            }
            taskDeletionStates.put(chatId, Collections.singletonList(taskId));
            sendDeleteConfirmationMessage(chatId, taskId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Ошибка при удалении задачи.");
        }
    }

    private void handleChangeStatus(String data, String chatId) {
        String taskIdString = data.substring("change_status_".length());
        try {
            int taskId = Integer.parseInt(taskIdString);
            Task task = taskService.findById(taskId);
            if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "Задача с указанным номером не найдена или не принадлежит вам.");
                return;
            }
            sendStatusChangeMessage(chatId, taskId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Ошибка при выборе задачи для изменения статуса.");
        }
    }

    private void handleStatusChange(String data, String chatId) {
        if (data.startsWith("status_completed_")) {
            boolean isCompleted = true;
            changeTaskStatus(chatId, data, isCompleted);
        } else if (data.startsWith("status_cancel_change_")) {
            sendMessage(chatId, "Изменение статуса задачи отменено.");
        }
    }

    private void handleDeleteReminder(String data, String chatId) {
        String reminderIdString = data.substring("delete_reminder_".length());
        try {
            Long reminderId = Long.parseLong(reminderIdString);
            Optional<Reminder> optionalReminder = reminderService.findReminderById(Math.toIntExact(reminderId));
            if (optionalReminder.isEmpty() || !optionalReminder.get().getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "Напоминание с указанным номером не найдено или не принадлежит вам.");
                return;
            }

            reminderDeletionStates.put(chatId, Collections.singletonList(reminderId));
            sendDeleteReminderConfirmationMessage(chatId, reminderId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Ошибка при удалении напоминания.");
        }
    }

    private void handleOtherStates(String data, String chatId) {
        TaskUpdateState currentState = taskUpdateStates.get(chatId);
        List<Integer> taskIds = taskDeletionStates.get(chatId);
        ReminderUpdateState curState = reminderUpdateStates.get(chatId);
        List<Long> remindersId = reminderDeletionStates.get(chatId);

        if (currentState == null && curState == null && (taskIds == null || taskIds.isEmpty()) && (remindersId == null || remindersId.isEmpty())) {
            sendMessage(chatId, "Ошибка при обработке запроса.");
            return;
        }

        switch (data) {
            case "update_title":
                assert currentState != null;
                currentState.setFieldToUpdate("title");
                sendNewValueRequest(chatId, "title");
                break;
            case "update_description":
                assert currentState != null;
                currentState.setFieldToUpdate("description");
                sendNewValueRequest(chatId, "description");
                break;
            case "update_message":
                assert curState != null;
                curState.setFieldToUpdate("message");
                sendNewValueRequestForReminder(chatId,"message");
                break;
            case "update_remind_at":
                assert curState != null;
                curState.setFieldToUpdate("remindAt");
                sendNewValueRequestForReminder(chatId,"remindAt");
                break;
            case "update_priority":
                assert currentState != null;
                currentState.setFieldToUpdate("priority");
                sendNewValueRequest(chatId, "priority");
                break;
            case "confirm_update":
                taskUpdateStates.remove(chatId);
                sendMessage(chatId, "Изменения подтверждены.");
                break;
            case "cancel_update", "update_cancel":
                assert currentState != null;
                cancelUpdate(chatId, currentState);
                break;
            case "confirm_update_reminder":
                reminderUpdateStates.remove(chatId);
                sendMessage(chatId, "Изменения подтверждены.");
                break;
            case "cancel_update_reminder", "update_cancel_reminder":
                assert curState != null;
                cancelReminderUpdate(chatId, curState);
                break;
            case "confirm_delete":
                if (taskIds != null && !taskIds.isEmpty()) {
                    confirmDelete(chatId, taskIds);
                    taskDeletionStates.remove(chatId);
                } else {
                    sendMessage(chatId, "Ошибка при подтверждении удаления задачи.");
                }
                break;
            case "cancel_delete":
                cancelDelete(chatId);
                break;
            case "confirm_delete_reminder":
                if (remindersId != null && !remindersId.isEmpty()) {
                    confirmDeleteReminder(chatId, remindersId);
                    taskDeletionStates.remove(chatId);
                } else {
                    sendMessage(chatId, "Ошибка при подтверждении удаления напоминания.");
                }
                break;
            case "cancel_delete_reminder":
                cancelDeleteReminder(chatId);
                break;
            default:
                sendMessage(chatId, "Неверный выбор.");
                break;
        }
    }

    private void handleChangeStatusCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals("\uD83D\uDCDB Изменить статус"))) {
            sendMessage(chatId, "Неверный формат команды. Используйте команду /change_status только без параметров.");
            return;
        }

        List<Task> tasks = taskService.findTasksByUserId(Long.parseLong(chatId));

        if (tasks.isEmpty()) {
            sendMessage(chatId, "У вас пока нет задач для изменения статуса.");
            return;
        }

        InlineKeyboardMarkup markup = createTasksStatusMarkup(tasks);

        SendMessage message = createMessage(chatId, "Выберите задачу для изменения статуса:", markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending task selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createTasksStatusMarkup(List<Task> tasks) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Task task : tasks) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(task.getTitle());
            button.setCallbackData("change_status_" + task.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void changeTaskStatus(String chatId, String data, boolean isCompleted) {
        String taskIdString = data.substring(data.lastIndexOf('_') + 1);
        try {
            int taskId = Integer.parseInt(taskIdString);
            Task task = taskService.findById(taskId);
            if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "Задача с указанным номером не найдена или не принадлежит вам.");
                return;
            }

            task.setCompleted(isCompleted);
            taskService.save(task);

            sendMessage(chatId, "Статус задачи '" + task.getTitle() + "' изменен на " + (isCompleted ? "Завершена" : "Не завершена") + ".");
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Ошибка при изменении статуса задачи.");
        }
    }

    private void sendStatusChangeMessage(String chatId, int taskId) {
        Task task = taskService.findById(taskId);
        if (task == null) {
            sendMessage(chatId, "Задача не найдена.");
            return;
        }

        StringBuilder statusMessage = new StringBuilder("Выберите новый статус для задачи:\n\n");
        statusMessage.append("Название: ").append(task.getTitle()).append("\n");
        statusMessage.append("Текущий статус: ").append(task.isCompleted() ? "Завершена" : "Не завершена").append("\n\n");
        statusMessage.append("Примечание: после смены статуса задачи на 'Завершена', задача будет удалена.\n");

        InlineKeyboardMarkup markup = createStatusChangeMarkup(taskId);

        SendMessage message = createMessage(chatId, statusMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending status change message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createStatusChangeMarkup(int taskId) {
        Task task = taskService.findById(taskId);
        if (task == null) {
            return null;
        }

        InlineKeyboardButton completedButton = new InlineKeyboardButton();
        completedButton.setText("Завершена");
        completedButton.setCallbackData("status_completed_" + taskId);

        InlineKeyboardButton cancelChangeButton = new InlineKeyboardButton();
        cancelChangeButton.setText("Отмена изменений");
        cancelChangeButton.setCallbackData("status_cancel_change_" + taskId);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(completedButton);
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(cancelChangeButton);

        keyboard.add(row1);
        keyboard.add(row2);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    @Scheduled(fixedRate = 1000)
    private void cleanupCompletedTasks() {
        List<Task> completedTasks = taskService.findByCompletedTrue();

        for (Task task : completedTasks) {
            taskService.delete(task);
        }
    }

    private void handleListTasksCommand(String chatId, String[] parts, String messageText) {
        if (parts.length > 1 && !messageText.equals("\uD83D\uDCCB Список задач")) {
            sendMessage(chatId, "Неверный формат команды. Используйте команду /list_tasks только без параметров.");
            return;
        }

        List<Task> tasks = taskService.getTasksByUserChatId(Long.parseLong(chatId));
        if (tasks.isEmpty()) {
            sendMessage(chatId, EmojiParser.parseToUnicode(":information_source: У вас нет задач."));
            return;
        }

        tasks.sort(Comparator.comparingInt(Task::getPriority).reversed());

        StringBuilder messageBuilder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Task task : tasks) {
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDD8A Название: ")).append(task.getTitle()).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCC4 Описание: ")).append(task.getDescription() != null ? task.getDescription() : "Без описания").append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCC5 Создано: ")).append(task.getCreationTimestamp().toLocalDateTime().format(formatter)).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\u2B50 Приоритет: ")).append(task.getPriority()).append("\n");
            if (task.getDeadline() != null) {
                messageBuilder.append(EmojiParser.parseToUnicode("\u23F0 Дедлайн: ")).append(task.getDeadline().format(formatter)).append("\n");
            }
            messageBuilder.append("\n");
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageBuilder.toString());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: " + e.getMessage());
        }
    }


    private void handleSetDeadlineCommand(String chatId, String[] parts, String messageText) {
        if (parts.length > 1 && !(messageText.equals("\u23F0 Установить дедлайн"))) {
            sendMessage(chatId, "Неверный формат команды. Используйте команду /set_deadline только без параметров.");
            return;
        }

        List<Task> tasks = taskService.findTasksByUserId(Long.parseLong(chatId));

        if (tasks.isEmpty()) {
            sendMessage(chatId, "У вас нет задач для установки дедлайна.");
            return;
        }

        InlineKeyboardMarkup markup = createDeadlineTaskMarkup(tasks);
        SendMessage message = createMessage(chatId, "Выберите задачу для установки дедлайна:", markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending deadline task selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createDeadlineTaskMarkup(List<Task> tasks) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Task task : tasks) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(task.getTitle());
            button.setCallbackData("set_deadline_" + task.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void processDeadlineInput(String chatId, String deadlineInput) {
        int taskId = taskDeadlineStates.get(chatId);
        Task task = taskService.findById(taskId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime deadline;
        try {
            deadline = LocalDateTime.parse(deadlineInput, formatter);
        } catch (DateTimeParseException e) {
            sendMessage(chatId, "Неверный формат даты. Пожалуйста, введите дедлайн в формате ГГГГ-ММ-ДД ЧЧ:ММ.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (deadline.isBefore(now) || deadline.isEqual(now)) {
            sendMessage(chatId, "Дата дедлайна должна быть в будущем. Пожалуйста, введите корректную дату.");
            return;
        }

        task.setDeadline(deadline);
        taskService.save(task);
        taskDeadlineStates.remove(chatId);
        sendMessage(chatId, "Дедлайн установлен для задачи: " + task.getTitle());
    }

    private void handleReminderCreationCommand(String chatId) {
        reminderCreationStates.put(chatId, new ReminderCreationState());
        sendMessage(chatId, "Введите текст напоминания:");
    }

    private void processReminderCreation(String chatId, String messageText) {
        ReminderCreationState currentState = reminderCreationStates.get(chatId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        if (currentState.getState() == ReminderState.ENTER_MESSAGE) {
            currentState.setMessage(messageText);
            currentState.setState(ReminderState.ENTER_REMINDER_TIME);
            sendMessage(chatId, "Введите время напоминания в формате yyyy-MM-dd HH:mm");
        } else if (currentState.getState() == ReminderState.ENTER_REMINDER_TIME) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(messageText, formatter);

                LocalDateTime currentDateTime = LocalDateTime.now();
                if (localDateTime.isBefore(currentDateTime)) {
                    sendMessage(chatId, "Время напоминания не может быть в прошлом или текущее. Пожалуйста, введите корректное время.");
                    return;
                }

                Timestamp reminderTime = Timestamp.valueOf(localDateTime);
                currentState.setReminderTime(reminderTime);
                currentState.setState(ReminderState.CONFIRMATION);

                String confirmationMessage = "Вы ввели следующие данные:\n" +
                        "Сообщение: " + currentState.getMessage() + "\n" +
                        "Время напоминания: " + localDateTime.format(formatter) + "\n\n" +
                        "Все верно?";
                sendConfirmationMessage(chatId, confirmationMessage);
            } catch (DateTimeParseException e) {
                sendMessage(chatId, "Неверный формат времени. Пожалуйста, введите время в формате yyyy-MM-dd HH:mm:");
            }
        }
    }

    private void sendConfirmationMessage(String chatId, String confirmationMessage) {
        InlineKeyboardMarkup markup = createConfirmationMarkupForRemind();

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(confirmationMessage);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createConfirmationMarkupForRemind() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButtonForRemind("Да", "confirm_yes"));
        row1.add(createInlineButtonForRemind("Нет", "confirm_no"));
        keyboard.add(row1);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private InlineKeyboardButton createInlineButtonForRemind(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private void createReminder(String message, Timestamp reminderTime, String chatId) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "Пользователь не зарегистрирован. Используйте /start для регистрации.");
            return;
        }

        Reminder reminder = new Reminder();
        reminder.setMessage(message);
        reminder.setReminderTime(reminderTime);
        reminder.setUser(user);

        reminderService.save(reminder);
    }

    @Scheduled(fixedRate = 1000)
    public void checkAndSendReminders() {
        List<Reminder> dueReminders = reminderService.findDueReminders();
        for (Reminder reminder : dueReminders) {
            if (!reminder.isSent()) {
                sendReminderNotification(reminder);
                reminder.setSent(true);
                reminderService.save(reminder);
            }
        }
    }

    private void sendReminderNotification(Reminder reminder) {
        String chatId = reminder.getUser().getChatId().toString();
        String messageText = "Напоминание: " + reminder.getMessage();

        InlineKeyboardMarkup markup = createReschedulingMarkup(Math.toIntExact(reminder.getId()));

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending reminder notification: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createReschedulingMarkup(int reminderId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(Arrays.asList(
                createInlineButtonForRemind("🕒 Отложить на 5 минут", "reschedule_5m_" + reminderId),
                createInlineButtonForRemind("⏰ Отложить на 1 час", "reschedule_1h_" + reminderId)
        ));
        keyboard.add(Arrays.asList(
                createInlineButtonForRemind("📅 Отложить на 1 день", "reschedule_1d_" + reminderId),
                createInlineButtonForRemind("⏱️ Задать время", "reschedule_custom_" + reminderId)
        ));
        keyboard.add(Collections.singletonList(
                createInlineButtonForRemind("✅ Ок!", "deleteOk_" + reminderId)
        ));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    public void rescheduleReminder(String chatId, int reminderId, Duration duration) {
        Optional<Reminder> optionalReminder = reminderService.findReminderById(reminderId);
        if (optionalReminder.isPresent()) {
            Reminder reminder = optionalReminder.get();
            if (reminder.getUser().getChatId().equals(Long.parseLong(chatId))) {
                Timestamp newTime = new Timestamp(reminder.getReminderTime().getTime() + duration.toMillis());
                reminder.setReminderTime(newTime);
                reminder.setSent(false);
                reminderService.save(reminder);
                sendMessage(chatId, "Напоминание отложено на " + duration.toMinutes() + " минут.");
            } else {
                sendMessage(chatId, "Ошибка при отложении напоминания.");
            }
        } else {
            sendMessage(chatId, "Напоминание не найдено.");
        }
    }

    public void askForCustomTime(String chatId, int reminderId) {
        reminderCustomTimeStates.put(chatId, reminderId);
        sendMessage(chatId, "Введите новое время напоминания в формате yyyy-MM-dd HH:mm:");
    }

    public void processCustomTimeInput(String chatId, String messageText) {
        Integer reminderId = reminderCustomTimeStates.get(chatId);
        if (reminderId != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime localDateTime = LocalDateTime.parse(messageText, formatter);

                LocalDateTime currentDateTime = LocalDateTime.now();
                if (localDateTime.isBefore(currentDateTime)) {
                    sendMessage(chatId, "Время напоминания не может быть в прошлом или текущее. Пожалуйста, введите корректное время.");
                    return;
                }

                Optional<Reminder> optionalReminder = reminderService.findReminderById(reminderId);
                if (optionalReminder.isPresent()) {
                    Reminder reminder = optionalReminder.get();
                    Timestamp newTime = Timestamp.valueOf(localDateTime);
                    reminder.setReminderTime(newTime);
                    reminder.setSent(false);
                    reminderService.save(reminder);
                    sendMessage(chatId, "Напоминание отложено на " + localDateTime.format(formatter) + ".");
                    reminderCustomTimeStates.remove(chatId);
                } else {
                    sendMessage(chatId, "Ошибка при отложении напоминания.");
                }
            } catch (DateTimeParseException e) {
                sendMessage(chatId, "Неверный формат времени. Пожалуйста, введите время в формате yyyy-MM-dd HH:mm:");
            }
        } else {
            sendMessage(chatId, "Ошибка при отложении напоминания.");
        }
    }

    private void deleteReminder(String chatId, int reminderId) {
        reminderService.deleteById(reminderId);
        sendMessage(chatId, "Напоминание выполнено и удалено.");
    }

    private void handleUpdateReminderCommand(String[] parts, String chatId) {
        if (parts.length > 1) {
            sendMessage(chatId, "Неверный формат команды. Используйте команду /update_reminder только без параметров.");
            return;
        }

        List<Reminder> reminders = reminderService.findRemindersByUserId(Long.parseLong(chatId));

        if (reminders.isEmpty()) {
            sendMessage(chatId, "У вас пока нет напоминаний для обновления.");
            return;
        }

        InlineKeyboardMarkup markup = createRemindersMarkup(reminders);

        SendMessage message = createMessage(chatId, "Выберите напоминание для обновления:", markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending reminder selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createRemindersMarkup(List<Reminder> reminders) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Reminder reminder : reminders) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Напоминание #" + reminder.getId());
            button.setCallbackData("update_reminder_" + reminder.getId());
            List<InlineKeyboardButton> row = Collections.singletonList(button);
            keyboard.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void sendFieldSelectionMessageForReminder(String chatId) {
        ReminderUpdateState currentState = reminderUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "Ошибка обновления напоминания.");
            return;
        }

        int reminderId = Math.toIntExact(currentState.getReminderId());

        Optional<Reminder> optionalReminder = reminderService.findReminderById(reminderId);
        if (optionalReminder.isEmpty()) {
            sendMessage(chatId, "Напоминание с указанным номером не найдено или не принадлежит вам.");
            return;
        }

        Reminder reminder = optionalReminder.get();

        if (!reminder.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "Напоминание не принадлежит вам.");
            return;
        }

        String currentMessage = reminder.getMessage();
        LocalDateTime remindAt = reminder.getReminderTime().toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        String selectionMessage = "Выберите, что вы хотите обновить для напоминания:\n";
        selectionMessage += "Текущее сообщение: " + currentMessage + "\n";
        selectionMessage += "Дата напоминания: " + remindAt.format(formatter) + "\n";

        InlineKeyboardMarkup markup = createReminderUpdateMarkup();

        SendMessage message = createMessage(chatId, selectionMessage, markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending field selection message for reminder: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createReminderUpdateMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton(BUTTON_MESSAGE, "update_message"));
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton(BUTTON_REMIND_AT, "update_remind_at"));
        row2.add(createInlineButton("Отмена", "update_cancel_reminder"));
        keyboard.add(row2);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void processFieldAndValueForReminder(String chatId, String messageText) {
        ReminderUpdateState currentState = reminderUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "Ошибка при обновлении напоминания.");
            return;
        }

        int reminderId = Math.toIntExact(currentState.getReminderId());

        Optional<Reminder> optionalReminder = reminderService.findReminderById(reminderId);
        if (optionalReminder.isEmpty()) {
            sendMessage(chatId, "Напоминание с указанным номером не найдено или не принадлежит вам.");
            reminderUpdateStates.remove(chatId);
            return;
        }

        Reminder reminder = optionalReminder.get();
        if (!reminder.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "Напоминание не принадлежит вам.");
            return;
        }

        String fieldToUpdate = currentState.getFieldToUpdate();

        switch (fieldToUpdate) {
            case "message":
                reminder.setMessage(messageText);
                break;
            case "remindAt":
                try {
                    LocalDateTime remindAt = LocalDateTime.parse(messageText, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    reminder.setReminderTime(Timestamp.valueOf(remindAt));
                } catch (DateTimeParseException e) {
                    sendMessage(chatId, "Неверный формат даты и времени. Используйте формат yyyy-MM-dd HH:mm");
                    return;
                }
                break;
            default:
                sendMessage(chatId, "Неизвестное поле для обновления: " + fieldToUpdate);
                return;
        }

        reminderService.save(reminder);

        sendConfirmationMessageForReminder(chatId, reminder);
    }

    private void sendConfirmationMessageForReminder(String chatId, Reminder reminder) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("Изменения сохранены:\n");
        confirmationMessage.append("Сообщение: ").append(reminder.getMessage()).append("\n");

        LocalDateTime remindAt = reminder.getReminderTime().toLocalDateTime();
        if (remindAt != null) {
            confirmationMessage.append("Напомнить в: ").append(remindAt.format(formatter)).append("\n");
        }

        confirmationMessage.append("\n\nПодтвердить изменения?");

        InlineKeyboardMarkup markup = createReminderConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending confirmation message for reminder: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createReminderConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow("Да", "confirm_update_reminder");
        row.add(createInlineButton("Нет", "cancel_update_reminder"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void cancelReminderUpdate(String chatId, ReminderUpdateState currentState) {
        reminderService.save(currentState.getOriginalReminder());
        reminderUpdateStates.remove(chatId);
        sendMessage(chatId, "Изменения в напоминании отменены.");
    }

    private void sendNewValueRequestForReminder(String chatId, String field) {
        String messageText;
        switch (field) {
            case "message":
                messageText = "Введите новое сообщение для напоминания:";
                break;
            case "remindAt":
                messageText = "Введите новую дату и время напоминания в формате yyyy-MM-dd HH:mm";
                break;
            default:
                log.error("Unsupported field type for reminder: {}", field);
                return;
        }

        sendMessage(chatId, messageText);
    }

    private void handleDeleteReminderCommand(String[] parts, String chatId) {
        if (parts.length > 1) {
            sendMessage(chatId, "Неверный формат команды. Используйте /delete_reminder без параметров.");
            return;
        }

        List<Reminder> reminders = reminderService.findRemindersByUserId(Long.parseLong(chatId));

        if (reminders.isEmpty()) {
            sendMessage(chatId, "У вас нет напоминаний для удаления.");
            return;
        }

        InlineKeyboardMarkup markup = createDeleteReminderMarkup(reminders);

        SendMessage message = createMessage(chatId, "Выберите напоминание для удаления:", markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending delete reminder selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createDeleteReminderMarkup(List<Reminder> reminders) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Reminder reminder : reminders) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Напоминание #" + reminder.getId());
            button.setCallbackData("delete_reminder_" + reminder.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void sendDeleteReminderConfirmationMessage(String chatId, Long reminderIdToDelete) {
        Optional<Reminder> optionalReminder = reminderService.findReminderById(reminderIdToDelete.intValue());
        if (!optionalReminder.isPresent()) {
            sendMessage(chatId, "Напоминание не найдено.");
            return;
        }

        Reminder reminder = optionalReminder.get();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("Вы уверены, что хотите удалить следующее напоминание?\n\n");
        confirmationMessage.append("Сообщение: ").append(reminder.getMessage()).append("\n");
        confirmationMessage.append("Время напоминания: ").append(reminder.getReminderTime().toLocalDateTime().format(formatter)).append("\n");

        InlineKeyboardMarkup markup = createDeleteReminderConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending delete reminder confirmation message: {}", e.getMessage());
        }
    }



    private InlineKeyboardMarkup createDeleteReminderConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow(BUTTON_CONFIRM, "confirm_delete_reminder");
        row.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_delete_reminder"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void confirmDeleteReminder(String chatId, List<Long> reminderIdsToDelete) {
        for (Long reminderId : reminderIdsToDelete) {
            reminderService.deleteReminderById(reminderId);
        }
        reminderDeletionStates.remove(chatId);
        sendMessage(chatId, "Напоминание удалено.");
    }

    private void cancelDeleteReminder(String chatId) {
        reminderDeletionStates.remove(chatId);
        sendMessage(chatId, "Удаление напоминания отменено.");
    }

    private void handleListRemindersCommand(String chatId) {
        List<Reminder> reminders = reminderService.getRemindersByUserChatId(Long.parseLong(chatId));

        if (reminders.isEmpty()) {
            sendMessage(chatId, EmojiParser.parseToUnicode(":information_source: У вас нет напоминаний."));
            return;
        }

        StringBuilder messageBuilder = new StringBuilder(EmojiParser.parseToUnicode("*Ваши напоминания:*\n\n"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Reminder reminder : reminders) {
            messageBuilder.append(EmojiParser.parseToUnicode(":bell: *Номер напоминания:* ")).append(reminder.getId()).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode(":memo: *Текст напоминания:* ")).append(reminder.getMessage()).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode(":alarm_clock: *Время напоминания:* ")).append(reminder.getReminderTime().toLocalDateTime().format(formatter)).append("\n");
            messageBuilder.append("\n");
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageBuilder.toString());
        message.setParseMode("Markdown");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: " + e.getMessage());
        }
    }
}


