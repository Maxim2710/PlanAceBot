package com.PlanAceBot.service;

import com.PlanAceBot.model.Reminder;
import com.PlanAceBot.model.Task;
import com.PlanAceBot.model.User;
import com.PlanAceBot.state.*;
import com.PlanAceBot.config.BotConfig;
import com.vdurmont.emoji.EmojiParser;
import lombok.Setter;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private static final String HELP_TEXT = ":information_source: Список доступных команд:\n\n" +
            "/start - Регистрация пользователя и приветственное сообщение.\n" +
            "/create_task - Создание новой задачи.\n" +
            "/update_task - Обновление существующей задачи.\n" +
            "/delete_task - Удаление задачи.\n" +
            "/change_status_task - Смена существующей задачи.\n" +
            "/help - Показать инструкцию по командам.\n" +
            "/list_tasks - Показать все задачи пользователя.\n" +
            "/set_deadline_task - Установить дедлайн для задачи.\n" +
            "/create_reminder - Создание нового напоминания.\n\n";

    private static final String COMMAND_START = "/start";
    private static final String COMMAND_CREATE = "/create_task";
    private static final String COMMAND_UPDATE = "/update_task";
    private static final String COMMAND_DELETE = "/delete_task";
    private static final String COMMAND_CHANGE_STATUS = "/change_status_task";
    private static final String COMMAND_HELP = "/help";
    private static final String COMMAND_LIST_TASKS = "/list_tasks";
    private static final String COMMAND_SET_DEADLINE = "/set_deadline_task";
    private static final String COMMAND_CREATE_REMINDER = "/create_reminder";

    private static final String BUTTON_TITLE = "Название";
    private static final String BUTTON_DESCRIPTION = "Описание";
    private static final String BUTTON_PRIORITY = "Приоритет";
    private static final String BUTTON_CANCEL = "Отмена";
    private static final String BUTTON_CONFIRM = "Да";
    private static final String BUTTON_CANCEL_UPDATE = "Нет";
    private static final String BUTTON_SUBSCRIBE = "Подписаться";
    private static final String BUTTON_CHECK_SUBSCRIPTION = "Проверить подписку";
    private static final String CHANNEL_NAME = "development_max";
    private static final String CHANNEL_USERNAME = "@development_max";

    private Map<String, TaskCreationState> taskCreationStates = new HashMap<>();
    private Map<String, TaskUpdateState> taskUpdateStates = new HashMap<>();
    private Map<String, List<Integer>> taskDeletionStates = new HashMap<>();
    private Map<String, Integer> taskDeadlineStates = new HashMap<>();
    private Map<String, ReminderCreationState> reminderCreationStates = new HashMap<>();
    private final Map<String, Integer> reminderCustomTimeStates = new ConcurrentHashMap<>();

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
            } else {
                switch (command) {
                    case COMMAND_START:
                        registerUserAndSendWelcomeMessage(chatId);
                        break;

                    case COMMAND_CREATE:
                        handleTaskCreationCommand(chatId, parts);
                        break;

                    case COMMAND_UPDATE:
                        handleUpdateCommand(parts, chatId);
                        break;

                    case COMMAND_DELETE:
                        handleDeleteCommand(parts, chatId);
                        break;

                    case COMMAND_CHANGE_STATUS:
                        handleChangeStatusCommand(parts, chatId);
                        break;

                    case COMMAND_HELP:
                        sendHelpMessage(chatId);
                        break;

                    case COMMAND_LIST_TASKS:
                        handleListTasksCommand(chatId);
                        break;

                    case COMMAND_SET_DEADLINE:
                        handleSetDeadlineCommand(chatId);
                        break;

                    case COMMAND_CREATE_REMINDER:
                        handleReminderCreationCommand(chatId);
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

    private void sendHelpMessage(String chatId) {
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

    private void registerUserAndSendWelcomeMessage(String chatId) {
        boolean isNewUser = false;

        if (!userService.existByChatId(Long.parseLong(chatId))) {
            User currentUser = new User();
            currentUser.setChatId(Long.parseLong(chatId));
            currentUser.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userService.save(currentUser);
            isNewUser = true;
        }

        if (isNewUser) {
            sendWelcomeMessage(chatId);
        } else {
            sendWelcomeBackMessage(chatId);
        }
    }

    private void sendWelcomeMessage(String chatId) {
        String welcomeMessage = EmojiParser.parseToUnicode("Добро пожаловать! Я бот для управления задачами. :blush:\n" +
                "Используйте команду /help, чтобы увидеть список доступных команд.");
        sendMessage(chatId, welcomeMessage);
    }

    private void sendWelcomeBackMessage(String chatId) {
        String welcomeBackMessage = EmojiParser.parseToUnicode("С возвращением! :blush:\n" +
                "Используйте команду /help, чтобы увидеть список доступных команд.");
        sendMessage(chatId, welcomeBackMessage);
    }

    private void handleTaskCreationCommand(String chatId, String[] parts) {
        if (parts.length == 1) {
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

    private void handleUpdateCommand(String[] parts, String chatId) {
        if (parts.length > 1) {
            sendMessage(chatId, "Неверный формат команды. Используйте только /update_task без параметров.");
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

    private void handleDeleteCommand(String[] parts, String chatId) {
        if (parts.length > 1) {
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
                registerUserAndSendWelcomeMessage(chatId);
            } else {
                sendMessage(chatId, "Вы еще не подписались на канал. Пожалуйста, подпишитесь и нажмите \"Проверить подписку\".");
            }
        } else if (data.startsWith("reschedule_")) {
            handleReschedule(data, chatId);
        } else if (data.startsWith("delete_")) {
            int reminderId = Integer.parseInt(data.split("_")[1]);
            deleteReminder(chatId, reminderId);
        } else if ("confirm_yes".equals(data)) {
            ReminderCreationState currentState = reminderCreationStates.get(chatId);
            if (currentState != null) {
                createReminder(currentState.getMessage(), currentState.getReminderTime(), chatId);
                reminderCreationStates.remove(chatId);

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String formattedReminderTime = formatter.format(currentState.getReminderTime());

                sendMessage(chatId, "Напоминание установлено на " + formattedReminderTime + ".");
            }
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

    private void handleOtherStates(String data, String chatId) {
        TaskUpdateState currentState = taskUpdateStates.get(chatId);
        List<Integer> taskIds = taskDeletionStates.get(chatId);

        if (currentState == null && (taskIds == null || taskIds.isEmpty())) {
            sendMessage(chatId, "Ошибка при обработке запроса.");
            return;
        }

        switch (data) {
            case "update_title":
                currentState.setFieldToUpdate("title");
                sendNewValueRequest(chatId, "title");
                break;
            case "update_description":
                currentState.setFieldToUpdate("description");
                sendNewValueRequest(chatId, "description");
                break;
            case "update_priority":
                currentState.setFieldToUpdate("priority");
                sendNewValueRequest(chatId, "priority");
                break;
            case "confirm_update":
                taskUpdateStates.remove(chatId);
                sendMessage(chatId, "Изменения подтверждены.");
                break;
            case "cancel_update", "update_cancel":
                cancelUpdate(chatId, currentState);
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
            default:
                sendMessage(chatId, "Неверный выбор.");
                break;
        }
    }

    private void handleChangeStatusCommand(String[] parts, String chatId) {
        if (parts.length > 1) {
            sendMessage(chatId, "Неверный формат команды. Используйте только /change_status без параметров.");
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

    private void handleListTasksCommand(String chatId) {
        List<Task> tasks = taskService.getTasksByUserChatId(Long.parseLong(chatId));
        if (tasks.isEmpty()) {
            sendMessage(chatId, EmojiParser.parseToUnicode(":information_source: У вас нет задач."));
            return;
        }

        tasks.sort(Comparator.comparingInt(Task::getPriority).reversed());

        StringBuilder messageBuilder = new StringBuilder(EmojiParser.parseToUnicode("*Ваши задачи:*\n\n"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Task task : tasks) {
            messageBuilder.append(EmojiParser.parseToUnicode(":pencil2: *Название:* ")).append(task.getTitle()).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode(":page_facing_up: *Описание:* ")).append(task.getDescription()).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode(":calendar: *Создано:* ")).append(task.getCreationTimestamp().toLocalDateTime().format(formatter)).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode(":star: *Приоритет:* ")).append(task.getPriority()).append("\n");
            if (task.getDeadline() != null) {
                messageBuilder.append(EmojiParser.parseToUnicode(":alarm_clock: *Дедлайн:* ")).append(task.getDeadline().format(formatter)).append("\n");
            }
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

    private void handleSetDeadlineCommand(String chatId) {
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
                createInlineButtonForRemind("✅ Ок!", "delete_" + reminderId)
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
}
