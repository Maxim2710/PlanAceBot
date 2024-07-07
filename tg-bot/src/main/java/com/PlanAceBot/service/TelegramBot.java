package com.PlanAceBot.service;

import com.PlanAceBot.model.*;
import com.PlanAceBot.state.*;
import com.PlanAceBot.config.BotConfig;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
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
            /calc - Калькулятор. Введите математическое выражение после команды.
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
            /show_reminder_commands - Отобразить все команды для взаимодействия с напоминаниями.
            /add_income - Создание нового дохода.
            /add_expense - Создание нового расхода.
            /update_income - Обновление существующей записи о доходе.
            /update_expense - Обновление существующей записи о расходе.
            /delete_income - Удаление записи о доходе.
            /delete_expense - Удаление записи о расходе.

            

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
    private static final String COMMAND_SHOW_REMINDER_COMMANDS = "/show_reminder_commands";

    private static final String COMMAND_ADD_INCOME = "/add_income";
    private static final String COMMAND_ADD_EXPENSE = "/add_expense";
    private static final String COMMAND_UPDATE_INCOME = "/update_income";
    private static final String COMMAND_UPDATE_EXPENSE = "/update_expense";
    private static final String COMMAND_DELETE_INCOME = "/delete_income";
    private static final String COMMAND_DELETE_EXPENSE = "/delete_expense";

    private static final String COMMAND_CALC = "/calc";

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

    private final Map<String, IncomeCreationState> incomeCreationStates = new HashMap<>();
    private final Map<String, ExpenseCreationState> expenseCreationStates = new HashMap<>();
    private final Map<String, IncomeUpdateState> incomeUpdateStates = new HashMap<>();
    private final Map<String, ExpenseUpdateState> expenseUpdateStates = new HashMap<>();
    private final Map<String, List<Integer>> incomeDeletionStates = new HashMap<>();
    private final Map<String, List<Integer>> expenseDeletionStates = new HashMap<>();

    private final Map<String, Boolean> calcStates = new HashMap<>();

    @Autowired
    private UserService userService;

    @Autowired
    private BotConfig botConfig;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private IncomeService incomeService;

    @Autowired
    private ExpenseService expenseService;

    public TelegramBot(BotConfig config) {
        this.botConfig = config;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "Регистрация пользователя и приветственное сообщение"));
        listofCommands.add(new BotCommand("/calc", "Калькулятор. Введите математическое выражение после команды"));
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
        listofCommands.add(new BotCommand("/show_reminder_commands", "Отобразить все команды для взаимодействия с напоминаниями"));
        listofCommands.add(new BotCommand("/add_income", "Создание нового дохода"));
        listofCommands.add(new BotCommand("/add_expense", "Создание нового расхода"));
        listofCommands.add(new BotCommand("/update_income", "Обновление существующей записи о доходе"));
        listofCommands.add(new BotCommand("/update_expense", "Обновление существующей записи о расходе"));
        listofCommands.add(new BotCommand("/delete_income", "Удаление записи о доходе"));
        listofCommands.add(new BotCommand("/delete_expense", "Удаление записи о расходе"));

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
                case "\uD83D\uDCCB Задачи" -> COMMAND_SHOW_TASK_COMMANDS;
                case "\uD83D\uDD14 Напоминания" -> COMMAND_SHOW_REMINDER_COMMANDS;
                case "\uD83D\uDCDD Создать задачу" -> COMMAND_CREATE;
                case "\uD83D\uDD8A Обновить задачу" -> COMMAND_UPDATE;
                case "\uD83D\uDDD1 Удалить задачу" -> COMMAND_DELETE;
                case "\uD83D\uDCDB Изменить статус" -> COMMAND_CHANGE_STATUS;
                case "\uD83D\uDCCB Список задач" -> COMMAND_LIST_TASKS;
                case "\u23F0 Установить дедлайн" -> COMMAND_SET_DEADLINE;
                case "◀ Вернуться назад" -> COMMAND_START;
                case "\uD83D\uDCDD Создать напоминание" -> COMMAND_CREATE_REMINDER;
                case "\uD83D\uDD8A Обновить напоминание" -> COMMAND_UPDATE_REMINDER;
                case "\uD83D\uDDD1 Удалить напоминание" -> COMMAND_DELETE_REMINDER;
                case "\uD83D\uDCCB Список напоминаний" -> COMMAND_LIST_REMINDERS;
                default -> messageText.split(" ", 2)[0];
            };

            if (taskCreationStates.containsKey(chatId)) {
                processTaskCreation(chatId, messageText);
            } else if (calcStates.getOrDefault(chatId, false)) {
                handleCalculateExpression(chatId, messageText);
                calcStates.put(chatId, false);
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
            } else if (incomeCreationStates.containsKey(chatId)) {
                processIncomeCreation(chatId, messageText);
            } else if (expenseCreationStates.containsKey(chatId)) {
                processExpenseCreation(chatId, messageText);
            } else if (incomeUpdateStates.containsKey(chatId)) {
                processFieldAndValueForIncome(chatId, messageText);
            } else if (expenseUpdateStates.containsKey(chatId)) {
                processFieldAndValueForExpense(chatId, messageText);
            } else if (incomeDeletionStates.containsKey(chatId)) {
                sendDeleteIncomeConfirmationMessage(chatId, incomeDeletionStates.get(chatId).get(0));
            } else if (expenseDeletionStates.containsKey(chatId)) {
                sendDeleteExpenseConfirmationMessage(chatId, expenseDeletionStates.get(chatId).get(0));
            } else {
                switch (command) {
                    case COMMAND_START:
                        registerUserAndSendWelcomeMessage(chatId, !messageText.equals("◀ Вернуться назад"));
                        break;

                    case COMMAND_CALC:
                        sendMessage(chatId, "Пожалуйста, введите математическое выражение для вычисления:");
                        calcStates.put(chatId, true);
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
                        handleReminderCreationCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_UPDATE_REMINDER:
                        handleUpdateReminderCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_DELETE_REMINDER:
                        handleDeleteReminderCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_LIST_REMINDERS:
                        handleListRemindersCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_SHOW_TASK_COMMANDS:
                        showCommandsKeyboard(chatId);
                        break;

                    case COMMAND_SHOW_REMINDER_COMMANDS:
                        showReminderCommandsKeyboard(chatId);
                        break;

                    case COMMAND_ADD_INCOME:
                        handleIncomeCreationCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_ADD_EXPENSE:
                        handleExpenseCreationCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_UPDATE_INCOME:
                        handleUpdateIncomeCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_UPDATE_EXPENSE:
                        handleUpdateExpenseCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_DELETE_INCOME:
                        handleDeleteIncomeCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_DELETE_EXPENSE:
                        handleDeleteExpenseCommand(parts, chatId, messageText);
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

    private void showReminderCommandsKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createKeyboardRow("\uD83D\uDCDD Создать напоминание", "\uD83D\uDD8A Обновить напоминание", "\uD83D\uDDD1 Удалить напоминание"));
        keyboard.add(createKeyboardRow("\uD83D\uDCCB Список напоминаний"));
        keyboard.add(createKeyboardRow("◀ Вернуться назад"));

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите команду для напоминаний:")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending reminder commands keyboard: {}", e.getMessage());
        }
    }

    private KeyboardRow createKeyboardRow(String... buttons) {
        KeyboardRow row = new KeyboardRow();
        for (String button : buttons) {
            row.add(button);
        }
        return row;
    }

    private void handleCalculateExpression(String chatId, String expression) {
        try {
            Expression e = new ExpressionBuilder(expression).build();
            double result = e.evaluate();
            sendMessage(chatId, "Результат: " + result);
        } catch (Exception ex) {
            sendMessage(chatId, "Ошибка в выражении. Пожалуйста, проверьте правильность ввода.");
        }
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

        KeyboardRow taskCommandsRow = new KeyboardRow();
        taskCommandsRow.add("\uD83D\uDCCB Задачи");

        KeyboardRow reminderCommandsRow = new KeyboardRow();
        reminderCommandsRow.add("\uD83D\uDD14 Напоминания");

        keyboard.add(taskCommandsRow);
        keyboard.add(reminderCommandsRow);
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

        KeyboardRow taskCommandsRow = new KeyboardRow();
        taskCommandsRow.add("\uD83D\uDCCB Задачи");

        KeyboardRow reminderCommandsRow = new KeyboardRow();
        reminderCommandsRow.add("\uD83D\uDD14 Напоминания");

        keyboard.add(taskCommandsRow);
        keyboard.add(reminderCommandsRow);
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

        KeyboardRow taskCommandsRow = new KeyboardRow();
        taskCommandsRow.add("\uD83D\uDCCB Задачи");

        KeyboardRow reminderCommandsRow = new KeyboardRow();
        reminderCommandsRow.add("\uD83D\uDD14 Напоминания");

        keyboard.add(taskCommandsRow);
        keyboard.add(reminderCommandsRow);
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
        if (getUserTaskCount(chatId) > 20) {
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
        if (getUserTaskCount(chatId) > 20) {
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
        } else if (data.equals("Заработная плата") || data.equals("Пенсия") ||
                data.equals("Стипендия") || data.equals("Пособие") ||
                data.equals("Доход от продажи товаров")) {
            processIncomeCreation(chatId, data);
        } else if (data.equals("income_other")) {
            processIncomeCreation(chatId, "Другое");
        } else if (data.equals("Еда") || data.equals("Транспорт") ||
                data.equals("Развлечения") || data.equals("Коммунальные услуги") ||
                data.equals("Медицина")) {
            processExpenseCreation(chatId, data);
        } else if (data.equals("expense_other")) {
            processExpenseCreation(chatId, "Другое");
        } else if (data.startsWith("reschedule_")) {
            handleReschedule(data, chatId);
        } else if ("confirm_yes".equals(data)) {
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
        } else if (data.startsWith("update_income_")) {
            handleUpdateIncome(data, chatId);
        } else if (data.startsWith("update_expense_")) {
            handleUpdateExpense(data, chatId);
        } else if (data.startsWith("delete_income_")) {
            handleDeleteIncome(data, chatId);
        } else if (data.startsWith("delete_expense_")) {
            handleDeleteExpense(data, chatId);
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

    private void handleUpdateIncome(String data, String chatId) {
        String incomeIdString = data.substring("update_income_".length());
        try {
            Long incomeId = Long.parseLong(incomeIdString);
            Income income = incomeService.findById(incomeId);
            if (income == null || !income.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "Доход с указанным номером не найден или не принадлежит вам.");
                return;
            }
            incomeUpdateStates.put(chatId, new IncomeUpdateState(incomeId, "", income));
            sendFieldSelectionMessageForIncome(chatId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Ошибка при выборе дохода для обновления.");
        }
    }

    public void handleUpdateExpense(String data, String chatId) {
        String expenseIdString = data.substring("update_expense_".length());
        try {
            Long expenseId = Long.parseLong(expenseIdString);
            Expense expense = expenseService.findById(expenseId);
            if (expense == null || !expense.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "Расход с указанным номером не найден или не принадлежит вам.");
                return;
            }
            expenseUpdateStates.put(chatId, new ExpenseUpdateState(expenseId, expense,""));
            sendFieldSelectionMessageForExpense(chatId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Ошибка при выборе расхода для обновления.");
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

    private void handleDeleteIncome(String data, String chatId) {
        String incomeIdString = data.substring("delete_income_".length());
        try {
            int incomeId = Integer.parseInt(incomeIdString);
            Income income = incomeService.findById((long) incomeId);
            if (income == null || !income.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "Запись о доходе с указанным номером не найдена или не принадлежит вам.");
                return;
            }
            incomeDeletionStates.put(chatId, Collections.singletonList(incomeId));
            sendDeleteIncomeConfirmationMessage(chatId, incomeId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Ошибка при удалении записи о доходе.");
        }
    }

    private void handleDeleteExpense(String data, String chatId) {
        String expenseIdString = data.substring("delete_expense_".length());
        try {
            long expenseId = Long.parseLong(expenseIdString);
            Expense expense = expenseService.findById(expenseId);
            if (expense == null || !expense.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "Запись о расходе с указанным номером не найдена или не принадлежит вам.");
                return;
            }
            expenseDeletionStates.put(chatId, Collections.singletonList(Math.toIntExact(expenseId)));
            sendDeleteExpenseConfirmationMessage(chatId, Math.toIntExact(expenseId));
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Ошибка при удалении записи о расходе.");
        }
    }

    private void handleOtherStates(String data, String chatId) {
        TaskUpdateState currentState = taskUpdateStates.get(chatId);
        IncomeCreationState currentIncomeState = incomeCreationStates.get(chatId);
        List<Integer> taskIds = taskDeletionStates.get(chatId);
        ReminderUpdateState curState = reminderUpdateStates.get(chatId);
        List<Long> remindersId = reminderDeletionStates.get(chatId);
        IncomeUpdateState incomeState = incomeUpdateStates.get(chatId);
        ExpenseUpdateState expenseUpdateState = expenseUpdateStates.get(chatId);
        List<Integer> incomeIds = incomeDeletionStates.get(chatId);
        List<Integer> expenseIds = expenseDeletionStates.get(chatId);

        if (currentState == null && curState == null && currentIncomeState == null &&
                (taskIds == null || taskIds.isEmpty()) && (remindersId == null || remindersId.isEmpty()) &&
                incomeState == null && expenseUpdateState == null && (incomeIds == null || incomeIds.isEmpty()) &&
                (expenseIds == null || expenseIds.isEmpty())) {
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
                sendNewValueRequestForReminder(chatId, "message");
                break;
            case "update_remind_at":
                assert curState != null;
                curState.setFieldToUpdate("remindAt");
                sendNewValueRequestForReminder(chatId, "remindAt");
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
                    reminderDeletionStates.remove(chatId);
                } else {
                    sendMessage(chatId, "Ошибка при подтверждении удаления напоминания.");
                }
                break;
            case "cancel_delete_reminder":
                cancelDeleteReminder(chatId);
                break;
            case "update_title_income":
                assert incomeState != null;
                incomeState.setFieldToUpdate("title");
                sendNewValueRequestForIncome(chatId, "title");
                break;
            case "update_amount_income":
                assert incomeState != null;
                incomeState.setFieldToUpdate("amount");
                sendNewValueRequestForIncome(chatId, "amount");
                break;
            case "update_date_income":
                assert incomeState != null;
                incomeState.setFieldToUpdate("date");
                sendNewValueRequestForIncome(chatId, "date");
                break;
            case "update_description_income":
                assert incomeState != null;
                incomeState.setFieldToUpdate("description");
                sendNewValueRequestForIncome(chatId, "description");
                break;
            case "update_category_income":
                assert incomeState != null;
                incomeState.setFieldToUpdate("category");
                sendNewValueRequestForIncome(chatId, "category");
                break;
            case "update_cancel_income":
                incomeUpdateStates.remove(chatId);
                sendMessage(chatId, "Обновление дохода отменено.");
                break;
            case "cancel_update_income":
                assert incomeState != null;
                cancelIncomeUpdate(chatId, incomeState);
                break;
            case "confirm_update_income":
                incomeUpdateStates.remove(chatId);
                sendMessage(chatId, "Изменения подтверждены.");
                break;
            case "update_title_expense":
                assert expenseUpdateState != null;
                expenseUpdateState.setFieldToUpdate("title");
                sendNewValueRequestForExpense(chatId, "title");
                break;
            case "update_amount_expense":
                assert expenseUpdateState != null;
                expenseUpdateState.setFieldToUpdate("amount");
                sendNewValueRequestForExpense(chatId, "amount");
                break;
            case "update_expense_date":
                assert expenseUpdateState != null;
                expenseUpdateState.setFieldToUpdate("date");
                sendNewValueRequestForExpense(chatId, "date");
                break;
            case "update_description_expense":
                assert expenseUpdateState != null;
                expenseUpdateState.setFieldToUpdate("description");
                sendNewValueRequestForExpense(chatId, "description");
                break;
            case "update_category_expense":
                assert expenseUpdateState != null;
                expenseUpdateState.setFieldToUpdate("category");
                sendNewValueRequestForExpense(chatId, "category");
                break;
            case "update_cancel_expense":
                expenseUpdateStates.remove(chatId);
                sendMessage(chatId, "Обновление расхода отменено.");
                break;
            case "confirm_update_expense":
                expenseUpdateStates.remove(chatId);
                sendMessage(chatId, "Изменения подтверждены.");
                break;
            case "cancel_update_expense":
                assert incomeState != null;
                cancelExpenseUpdate(chatId, expenseUpdateState);
            case "confirm_delete_income":
                if (incomeIds != null && !incomeIds.isEmpty()) {
                    confirmDeleteIncome(chatId, incomeIds);
                    incomeDeletionStates.remove(chatId);
                } else {
                    sendMessage(chatId, "Ошибка при подтверждении удаления записи о доходе.");
                }
                break;
            case "cancel_delete_income":
                cancelDeleteIncome(chatId);
                break;
            case "confirm_delete_expense":
                if (expenseIds != null && !expenseIds.isEmpty()) {
                    confirmDeleteExpense(chatId, expenseIds);
                    expenseDeletionStates.remove(chatId);
                } else {
                    sendMessage(chatId, "Ошибка при подтверждении удаления записи о доходе.");
                }
                break;
            case "cancel_delete_expense":
                cancelDeleteExpense(chatId);
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

    private void handleReminderCreationCommand(String chatId, String[] parts, String messageText) {
        if (parts.length > 1 && !(messageText.equals("\uD83D\uDCDD Создать напоминание"))) {
            sendMessage(chatId, "Неверный формат команды. Используйте команду /create_reminder только без параметров.");
            return;
        }

        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "Пользователь не зарегистрирован. Используйте /start для регистрации.");
            return;
        }

        int existingRemindersCount = reminderService.countByUser(user);
        if (existingRemindersCount > 20) {
            sendMessage(chatId, "Вы уже создали максимальное количество напоминаний (20 штук).");
            return;
        }

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

    private void handleUpdateReminderCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals("\uD83D\uDD8A Обновить напоминание"))) {
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

    private void handleDeleteReminderCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals("\uD83D\uDDD1 Удалить напоминание"))) {
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

    private void handleListRemindersCommand(String chatId, String[] parts, String messageText) {
        if (parts.length > 1 && !(messageText.equals("\uD83D\uDCCB Список напоминаний"))) {
            sendMessage(chatId, "Неверный формат команды. Используйте /list_reminders без параметров.");
            return;
        }

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

    private void handleIncomeCreationCommand(String chatId, String[] parts, String messageText) {
        if (parts.length == 1 || messageText.equals("\uD83D\uDCB8 Добавить доход")) {
            startIncomeCreation(chatId);
        } else {
            sendMessage(chatId, "Неверный формат команды. Используйте /add_income без параметров.");
        }
    }

    private void startIncomeCreation(String chatId) {
        IncomeCreationState currentState = incomeCreationStates.get(chatId);
        if (currentState == null) {
            currentState = new IncomeCreationState();
            incomeCreationStates.put(chatId, currentState);
        }

        currentState.setState(IncomeState.ENTER_TITLE);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите тип дохода или введите своё значение:");
        message.setReplyMarkup(getIncomeSuggestionsInlineKeyboard());

        sendMessageForIncome(message);
    }

    private void processIncomeCreation(String chatId, String messageText) {
        IncomeCreationState currentState = incomeCreationStates.get(chatId);

        if (currentState.getState() == IncomeState.ENTER_TITLE) {
            if ("Заработная плата".equals(messageText) || "Пенсия".equals(messageText) ||
                    "Стипендия".equals(messageText) || "Пособие".equals(messageText) ||
                    "Доход от продажи товаров".equals(messageText)) {

                currentState.setTitle(messageText);
                currentState.setState(IncomeState.ENTER_AMOUNT);
                sendMessage(chatId, "Введите сумму дохода для '" + messageText + "':");

            } else if ("Другое".equals(messageText)) {
                currentState.setTitle("Другое");
                currentState.setState(IncomeState.ENTER_CUSTOM_TITLE);
                sendMessage(chatId, "Введите тип дохода:");

            } else {
                sendMessage(chatId, "Пожалуйста, выберите тип дохода из предложенных кнопок.");
            }

        } else if (currentState.getState() == IncomeState.ENTER_CUSTOM_TITLE) {
            currentState.setTitle(messageText);
            currentState.setState(IncomeState.ENTER_AMOUNT);
            sendMessage(chatId, "Введите сумму дохода для '" + messageText + "':");

        } else if (currentState.getState() == IncomeState.ENTER_AMOUNT) {
            try {
                double amount = Double.parseDouble(messageText);
                currentState.setAmount(amount);
                currentState.setState(IncomeState.ENTER_DATE);
                sendMessage(chatId, "Введите дату дохода (в формате ГГГГ-ММ-ДД):");
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Пожалуйста, введите корректное числовое значение для суммы дохода:");
            }
        } else if (currentState.getState() == IncomeState.ENTER_DATE) {
            try {
                Timestamp date = Timestamp.valueOf(messageText + " 00:00:00");
                currentState.setDate(date);
                currentState.setState(IncomeState.ENTER_DESCRIPTION);
                sendMessage(chatId, "Введите описание дохода:");
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, "Пожалуйста, введите корректную дату в формате ГГГГ-ММ-ДД:");
            }
        } else if (currentState.getState() == IncomeState.ENTER_DESCRIPTION) {
            currentState.setDescription(messageText);
            currentState.setState(IncomeState.ENTER_CATEGORY);
            sendMessage(chatId, "Введите категорию дохода:");
        } else if (currentState.getState() == IncomeState.ENTER_CATEGORY) {
            currentState.setCategory(messageText);

            createIncome(currentState.getTitle(), currentState.getAmount(), currentState.getDate(),
                    currentState.getDescription(), currentState.getCategory(), chatId);
            incomeCreationStates.remove(chatId);

            sendMessage(chatId, "Доход '" + currentState.getTitle() + "' с суммой " + currentState.getAmount() + " создан.");
        }
    }

    private void createIncome(String title, double amount, Timestamp date, String description, String category, String chatId) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "Пользователь не зарегистрирован. Используйте /start для регистрации.");
            return;
        }

        Income income = new Income();
        income.setTitle(title);
        income.setAmount(amount);
        income.setDate(date);
        income.setDescription(description);
        income.setCategory(category);
        income.setUser(user);

        incomeService.save(income);
    }

    public static InlineKeyboardMarkup getIncomeSuggestionsInlineKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1_1 = new InlineKeyboardButton();
        button1_1.setText("Заработная плата");
        button1_1.setCallbackData("Заработная плата");
        row1.add(button1_1);

        InlineKeyboardButton button1_2 = new InlineKeyboardButton();
        button1_2.setText("Пенсия");
        button1_2.setCallbackData("Пенсия");
        row1.add(button1_2);

        rowsInline.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2_1 = new InlineKeyboardButton();
        button2_1.setText("Стипендия");
        button2_1.setCallbackData("Стипендия");
        row2.add(button2_1);

        InlineKeyboardButton button2_2 = new InlineKeyboardButton();
        button2_2.setText("Пособие");
        button2_2.setCallbackData("Пособие");
        row2.add(button2_2);

        rowsInline.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3_1 = new InlineKeyboardButton();
        button3_1.setText("Доход от продажи товаров");
        button3_1.setCallbackData("Доход от продажи товаров");
        row3.add(button3_1);

        InlineKeyboardButton button3_2 = new InlineKeyboardButton();
        button3_2.setText("Другое");
        button3_2.setCallbackData("income_other");
        row3.add(button3_2);

        rowsInline.add(row3);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    public void sendMessageForIncome(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: {}", e.getMessage());
        }
    }

    private void handleExpenseCreationCommand(String chatId, String[] parts, String messageText) {
        if (parts.length == 1 || messageText.equals("\uD83D\uDCB0 Добавить расход")) {
            startExpenseCreation(chatId);
        } else {
            sendMessage(chatId, "Неверный формат команды. Используйте /add_expense без параметров.");
        }
    }

    private void startExpenseCreation(String chatId) {
        ExpenseCreationState currentState = expenseCreationStates.get(chatId);
        if (currentState == null) {
            currentState = new ExpenseCreationState();
            expenseCreationStates.put(chatId, currentState);
        }

        currentState.setState(ExpenseState.ENTER_TITLE);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите тип расхода или введите своё значение:");
        message.setReplyMarkup(getExpenseSuggestionsInlineKeyboard());

        sendMessageForExpense(message);
    }

    private void processExpenseCreation(String chatId, String messageText) {
        ExpenseCreationState currentState = expenseCreationStates.get(chatId);

        if (currentState.getState() == ExpenseState.ENTER_TITLE) {
            if ("Еда".equals(messageText) || "Транспорт".equals(messageText) ||
                    "Развлечения".equals(messageText) || "Коммунальные услуги".equals(messageText) ||
                    "Медицина".equals(messageText)) {

                currentState.setTitle(messageText);
                currentState.setState(ExpenseState.ENTER_AMOUNT);
                sendMessage(chatId, "Введите сумму расхода для '" + messageText + "':");

            } else if ("Другое".equals(messageText)) {
                currentState.setTitle("Другое");
                currentState.setState(ExpenseState.ENTER_CUSTOM_TITLE);
                sendMessage(chatId, "Введите тип расхода:");

            } else {
                sendMessage(chatId, "Пожалуйста, выберите тип расхода из предложенных кнопок.");
            }

        } else if (currentState.getState() == ExpenseState.ENTER_CUSTOM_TITLE) {
            currentState.setTitle(messageText);
            currentState.setState(ExpenseState.ENTER_AMOUNT);
            sendMessage(chatId, "Введите сумму расхода для '" + messageText + "':");

        } else if (currentState.getState() == ExpenseState.ENTER_AMOUNT) {
            try {
                double amount = Double.parseDouble(messageText);
                currentState.setAmount(amount);
                currentState.setState(ExpenseState.ENTER_DATE);
                sendMessage(chatId, "Введите дату расхода (в формате ГГГГ-ММ-ДД):");
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Пожалуйста, введите корректное числовое значение для суммы расхода:");
            }
        } else if (currentState.getState() == ExpenseState.ENTER_DATE) {
            try {
                Timestamp date = Timestamp.valueOf(messageText + " 00:00:00");
                currentState.setDate(date);
                currentState.setState(ExpenseState.ENTER_DESCRIPTION);
                sendMessage(chatId, "Введите описание расхода:");
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, "Пожалуйста, введите корректную дату в формате ГГГГ-ММ-ДД:");
            }
        } else if (currentState.getState() == ExpenseState.ENTER_DESCRIPTION) {
            currentState.setDescription(messageText);
            currentState.setState(ExpenseState.ENTER_CATEGORY);
            sendMessage(chatId, "Введите категорию расхода:");
        } else if (currentState.getState() == ExpenseState.ENTER_CATEGORY) {
            currentState.setCategory(messageText);
            createExpense(currentState.getTitle(), currentState.getAmount(), currentState.getDate(), currentState.getDescription(), currentState.getCategory(), chatId);
            expenseCreationStates.remove(chatId);
            sendMessage(chatId, "Расход '" + currentState.getTitle() + "' на сумму " + currentState.getAmount() + " создан.");
        }
    }

    private void createExpense(String title, double amount, Timestamp date, String description, String category, String chatId) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "Пользователь не зарегистрирован. Используйте /start для регистрации.");
            return;
        }

        Expense expense = new Expense();
        expense.setTitle(title);
        expense.setAmount(amount);
        expense.setDate(date);
        expense.setDescription(description);
        expense.setCategory(category);
        expense.setUser(user);

        expenseService.save(expense);
    }

    public static InlineKeyboardMarkup getExpenseSuggestionsInlineKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1_1 = new InlineKeyboardButton();
        button1_1.setText("Еда");
        button1_1.setCallbackData("Еда");
        row1.add(button1_1);

        InlineKeyboardButton button1_2 = new InlineKeyboardButton();
        button1_2.setText("Транспорт");
        button1_2.setCallbackData("Транспорт");
        row1.add(button1_2);

        rowsInline.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2_1 = new InlineKeyboardButton();
        button2_1.setText("Развлечения");
        button2_1.setCallbackData("Развлечения");
        row2.add(button2_1);

        InlineKeyboardButton button2_2 = new InlineKeyboardButton();
        button2_2.setText("Коммунальные услуги");
        button2_2.setCallbackData("Коммунальные услуги");
        row2.add(button2_2);

        rowsInline.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3_1 = new InlineKeyboardButton();
        button3_1.setText("Медицина");
        button3_1.setCallbackData("Медицина");
        row3.add(button3_1);

        InlineKeyboardButton button3_2 = new InlineKeyboardButton();
        button3_2.setText("Другое");
        button3_2.setCallbackData("expense_other");
        row3.add(button3_2);

        rowsInline.add(row3);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    public void sendMessageForExpense(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: {}", e.getMessage());
        }
    }

    private void handleUpdateIncomeCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !messageText.equals("\uD83D\uDD8A Обновить доход")) {
            sendMessage(chatId, "Неверный формат команды. Используйте команду /update_income только без параметров.");
            return;
        }

        List<Income> incomes = incomeService.findIncomesByUserId(Long.parseLong(chatId));

        if (incomes.isEmpty()) {
            sendMessage(chatId, "У вас пока нет доходов для обновления.");
            return;
        }

        InlineKeyboardMarkup markup = createIncomesMarkup(incomes);

        SendMessage message = createMessage(chatId, "Выберите доход для обновления:", markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending income selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createIncomesMarkup(List<Income> incomes) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Income income : incomes) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Запись о доходах №" + income.getId() + ": " + income.getTitle());
            button.setCallbackData("update_income_" + income.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void sendFieldSelectionMessageForIncome(String chatId) {
        IncomeUpdateState currentState = incomeUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "Ошибка обновления дохода.");
            return;
        }

        Long incomeId = currentState.getIncomeId();

        Income income = incomeService.findById(incomeId);
        if (income == null || !income.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "Доход с указанным номером не найден или не принадлежит вам.");
            return;
        }

        String currentTitle = income.getTitle();
        Double currentAmount = income.getAmount();
        Timestamp currentDate = income.getDate();
        String currentDescription = income.getDescription();
        String currentCategory = income.getCategory();

        String selectionMessage = "Выберите, что вы хотите обновить для дохода:\n";
        selectionMessage += "Текущее название: " + currentTitle + "\n";
        selectionMessage += "Текущая сумма: " + currentAmount + "\n";
        selectionMessage += "Дата дохода: " + currentDate + "\n";
        selectionMessage += "Описание: " + currentDescription + "\n";
        selectionMessage += "Категория: " + currentCategory + "\n";

        InlineKeyboardMarkup markup = createUpdateMarkupForIncome();

        SendMessage message = createMessage(chatId, selectionMessage, markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending field selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createUpdateMarkupForIncome() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton("Название", "update_title_income"));
        row1.add(createInlineButton("Сумма", "update_amount_income"));
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton("Дата", "update_income_date"));
        row2.add(createInlineButton("Описание", "update_description_income"));
        keyboard.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton("Категория", "update_category_income"));
        row3.add(createInlineButton("Отмена", "update_cancel_income"));
        keyboard.add(row3);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void processFieldAndValueForIncome(String chatId, String messageText) {
        IncomeUpdateState currentState = incomeUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "Ошибка при обновлении дохода.");
            return;
        }

        Long incomeId = currentState.getIncomeId();
        String fieldToUpdate = currentState.getFieldToUpdate();

        Income income = incomeService.findById(incomeId);
        if (income == null || !income.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "Доход с указанным номером не найден или не принадлежит вам.");
            incomeUpdateStates.remove(chatId);
            return;
        }

        try {
            switch (fieldToUpdate) {
                case "title":
                    income.setTitle(messageText);
                    break;
                case "amount":
                    double amount = Double.parseDouble(messageText);
                    income.setAmount(amount);
                    break;
                case "date":
                    Timestamp date = Timestamp.valueOf(messageText + " 00:00:00");
                    income.setDate(date);
                    break;
                case "description":
                    income.setDescription(messageText);
                    break;
                case "category":
                    income.setCategory(messageText);
                    break;
                default:
                    sendMessage(chatId, "Ошибка при обновлении дохода.");
                    incomeUpdateStates.remove(chatId);
                    return;
            }

            incomeService.save(income);

            sendIncomeUpdateConfirmationMessage(chatId, income);
        } catch (IllegalArgumentException e) {
            sendMessage(chatId, "Ошибка при обработке введенного значения. Попробуйте снова.");
        }
    }

    private void sendIncomeUpdateConfirmationMessage(String chatId, Income income) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("Изменения сохранены:\n");
        confirmationMessage.append("Название: ").append(income.getTitle()).append("\n");
        confirmationMessage.append("Описание: ").append(income.getDescription()).append("\n");
        confirmationMessage.append("Сумма: ").append(income.getAmount()).append("\n");
        confirmationMessage.append("Категория: ").append(income.getCategory()).append("\n");

        confirmationMessage.append("\nДата создания: ").append(income.getDate().toLocalDateTime().format(formatter));

        confirmationMessage.append("\n\nПодтвердить изменения?");

        InlineKeyboardMarkup markup = createIncomeConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createIncomeConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow(BUTTON_CONFIRM, "confirm_update_income");
        row.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_update_income"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void cancelIncomeUpdate(String chatId, IncomeUpdateState currentState) {
        incomeService.save(currentState.getOriginalIncome());
        incomeUpdateStates.remove(chatId);
        sendMessage(chatId, "Изменения отменены.");
    }

    private void sendNewValueRequestForIncome(String chatId, String field) {
        String fieldDisplayName = switch (field) {
            case "title" -> "название";
            case "amount" -> "сумма";
            case "date" -> "дата";
            case "description" -> "описание";
            case "category" -> "категория";
            default -> "";
        };
        sendMessage(chatId, "Введите новое значение для поля " + fieldDisplayName + ":");
    }

    public void handleUpdateExpenseCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !messageText.equals("\uD83D\uDD8A Обновить расход")) {
            sendMessage(chatId, "Неверный формат команды. Используйте команду /update_expense только без параметров.");
            return;
        }

        List<Expense> expenses = expenseService.findExpensesByUserId(Long.parseLong(chatId));

        if (expenses.isEmpty()) {
            sendMessage(chatId, "У вас пока нет расходов для обновления.");
            return;
        }

        InlineKeyboardMarkup markup = createExpensesMarkup(expenses);

        SendMessage message = createMessage(chatId, "Выберите расход для обновления:", markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending expense selection message: {}", e.getMessage());
        }
    }

    public InlineKeyboardMarkup createExpensesMarkup(List<Expense> expenses) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Expense expense : expenses) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Запись о расходах №" + expense.getId() + ": " + expense.getTitle());
            button.setCallbackData("update_expense_" + expense.getId());

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);

            keyboard.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    public void sendFieldSelectionMessageForExpense(String chatId) {
        ExpenseUpdateState currentState = expenseUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "Ошибка обновления расхода.");
            return;
        }

        Long expenseId = currentState.getExpenseId();

        Expense expense = expenseService.findById(expenseId);
        if (expense == null || !expense.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "Расход с указанным номером не найден или не принадлежит вам.");
            return;
        }

        String currentTitle = expense.getTitle();
        Double currentAmount = expense.getAmount();
        Timestamp currentDate = expense.getDate();
        String currentDescription = expense.getDescription();
        String currentCategory = expense.getCategory();

        String selectionMessage = "Выберите, что вы хотите обновить для расхода:\n";
        selectionMessage += "Текущее название: " + currentTitle + "\n";
        selectionMessage += "Текущая сумма: " + currentAmount + "\n";
        selectionMessage += "Дата расхода: " + currentDate + "\n";
        selectionMessage += "Описание: " + currentDescription + "\n";
        selectionMessage += "Категория: " + currentCategory + "\n";

        InlineKeyboardMarkup markup = createUpdateMarkupForExpense();

        SendMessage message = createMessage(chatId, selectionMessage, markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending field selection message for expense: {}", e.getMessage());
        }
    }

    public InlineKeyboardMarkup createUpdateMarkupForExpense() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton("Название", "update_title_expense"));
        row1.add(createInlineButton("Сумма", "update_amount_expense"));
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton("Дата", "update_expense_date"));
        row2.add(createInlineButton("Описание", "update_description_expense"));
        keyboard.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton("Категория", "update_category_expense"));
        row3.add(createInlineButton("Отмена", "update_cancel_expense"));
        keyboard.add(row3);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    public void processFieldAndValueForExpense(String chatId, String messageText) {
        ExpenseUpdateState currentState = expenseUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "Ошибка при обновлении расхода.");
            return;
        }

        Long expenseId = currentState.getExpenseId();
        String fieldToUpdate = currentState.getFieldToUpdate();

        Expense expense = expenseService.findById(expenseId);
        if (expense == null || !expense.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "Расход с указанным номером не найден или не принадлежит вам.");
            expenseUpdateStates.remove(chatId);
            return;
        }

        try {
            switch (fieldToUpdate) {
                case "title":
                    expense.setTitle(messageText);
                    break;
                case "amount":
                    double amount = Double.parseDouble(messageText);
                    expense.setAmount(amount);
                    break;
                case "date":
                    Timestamp date = Timestamp.valueOf(messageText + " 00:00:00");
                    expense.setDate(date);
                    break;
                case "description":
                    expense.setDescription(messageText);
                    break;
                case "category":
                    expense.setCategory(messageText);
                    break;
                default:
                    sendMessage(chatId, "Ошибка при обновлении расхода.");
                    expenseUpdateStates.remove(chatId);
                    return;
            }

            expenseService.save(expense);

            sendExpenseUpdateConfirmationMessage(chatId, expense);
        } catch (IllegalArgumentException e) {
            sendMessage(chatId, "Ошибка при обработке введенного значения. Попробуйте снова.");
        }
    }

    public void sendExpenseUpdateConfirmationMessage(String chatId, Expense expense) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("Изменения сохранены:\n");
        confirmationMessage.append("Название: ").append(expense.getTitle()).append("\n");
        confirmationMessage.append("Описание: ").append(expense.getDescription()).append("\n");
        confirmationMessage.append("Сумма: ").append(expense.getAmount()).append("\n");
        confirmationMessage.append("Категория: ").append(expense.getCategory()).append("\n");

        confirmationMessage.append("\nДата создания: ").append(expense.getDate().toLocalDateTime().format(formatter));

        confirmationMessage.append("\n\nПодтвердить изменения?");

        InlineKeyboardMarkup markup = createExpenseConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createExpenseConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow("Да", "confirm_update_expense");
        row.add(createInlineButton("Нет", "cancel_update_expense"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void cancelExpenseUpdate(String chatId, ExpenseUpdateState currentState) {
        expenseService.save(currentState.getOriginalExpense());
        expenseUpdateStates.remove(chatId);
        sendMessage(chatId, "Изменения отменены.");
    }

    private void sendNewValueRequestForExpense(String chatId, String field) {
        String fieldDisplayName = switch (field) {
            case "title" -> "название";
            case "amount" -> "сумма";
            case "date" -> "дата";
            case "description" -> "описание";
            case "category" -> "категория";
            default -> "";
        };
        sendMessage(chatId, "Введите новое значение для поля " + fieldDisplayName + ":");
    }

    private void handleDeleteIncomeCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals("\uD83D\uDDD1 Удалить доход"))) {
            sendMessage(chatId, "Неверный формат команды. Используйте /delete_income без параметров.");
            return;
        }

        List<Income> incomes = incomeService.findIncomesByUserId(Long.parseLong(chatId));

        if (incomes.isEmpty()) {
            sendMessage(chatId, "У вас нет доходов для удаления.");
            return;
        }

        InlineKeyboardMarkup markup = createDeleteIncomeMarkup(incomes);

        SendMessage message = createMessage(chatId, "Выберите доход для удаления:", markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending delete income selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createDeleteIncomeMarkup(List<Income> incomes) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Income income : incomes) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(income.getTitle());
            button.setCallbackData("delete_income_" + income.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void sendDeleteIncomeConfirmationMessage(String chatId, int incomeIdToDelete) {
        Income income = incomeService.findById((long) incomeIdToDelete);
        if (income == null) {
            sendMessage(chatId, "Запись о доходе не найдена.");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("Вы уверены, что хотите удалить следующую запись о доходе?\n\n");
        confirmationMessage.append("Название: ").append(income.getTitle()).append("\n");
        confirmationMessage.append("Сумма: ").append(income.getAmount()).append("\n");
        confirmationMessage.append("Категория: ").append(income.getCategory()).append("\n");
        confirmationMessage.append("Дата: ").append(income.getDate().toLocalDateTime().format(formatter)).append("\n");

        if (income.getDescription() != null && !income.getDescription().isEmpty()) {
            confirmationMessage.append("Описание: ").append(income.getDescription()).append("\n");
        }

        InlineKeyboardMarkup markup = createDeleteIncomeConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending delete confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createDeleteIncomeConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow(BUTTON_CONFIRM, "confirm_delete_income");
        row.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_delete_income"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void confirmDeleteIncome(String chatId, List<Integer> incomeIdsToDelete) {
        for (Integer incomeId : incomeIdsToDelete) {
            incomeService.delete(incomeId);
        }
        incomeDeletionStates.remove(chatId);
        sendMessage(chatId, "Запись о доходе удалена.");
    }

    private void cancelDeleteIncome(String chatId) {
        incomeDeletionStates.remove(chatId);
        sendMessage(chatId, "Удаление отменено.");
    }

    private void handleDeleteExpenseCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals("\uD83D\uDDD1 Удалить расход"))) {
            sendMessage(chatId, "Неверный формат команды. Используйте /delete_expense без параметров.");
            return;
        }

        List<Expense> expenses = expenseService.findExpensesByUserId(Long.parseLong(chatId));

        if (expenses.isEmpty()) {
            sendMessage(chatId, "У вас нет расходов для удаления.");
            return;
        }

        InlineKeyboardMarkup markup = createDeleteExpenseMarkup(expenses);

        SendMessage message = createMessage(chatId, "Выберите расход для удаления:", markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending delete expense selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createDeleteExpenseMarkup(List<Expense> expenses) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Expense expense : expenses) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(expense.getTitle());
            button.setCallbackData("delete_expense_" + expense.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void sendDeleteExpenseConfirmationMessage(String chatId, int expenseIdToDelete) {
        Expense expense = expenseService.findById((long) expenseIdToDelete);
        if (expense == null) {
            sendMessage(chatId, "Запись о расходе не найдена.");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("Вы уверены, что хотите удалить следующую запись о расходе?\n\n");
        confirmationMessage.append("Название: ").append(expense.getTitle()).append("\n");
        confirmationMessage.append("Сумма: ").append(expense.getAmount()).append("\n");
        confirmationMessage.append("Категория: ").append(expense.getCategory()).append("\n");
        confirmationMessage.append("Дата: ").append(expense.getDate().toLocalDateTime().format(formatter)).append("\n");

        if (expense.getDescription() != null && !expense.getDescription().isEmpty()) {
            confirmationMessage.append("Описание: ").append(expense.getDescription()).append("\n");
        }

        InlineKeyboardMarkup markup = createDeleteExpenseConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending delete confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createDeleteExpenseConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow(BUTTON_CONFIRM, "confirm_delete_expense");
        row.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_delete_expense"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void confirmDeleteExpense(String chatId, List<Integer> expenseIdsToDelete) {
        for (Integer expenseId : expenseIdsToDelete) {
            expenseService.delete(Long.valueOf(expenseId));
        }
        expenseDeletionStates.remove(chatId);
        sendMessage(chatId, "Запись о расходе удалена.");
    }

    private void cancelDeleteExpense(String chatId) {
        expenseDeletionStates.remove(chatId);
        sendMessage(chatId, "Удаление отменено.");
    }
}


