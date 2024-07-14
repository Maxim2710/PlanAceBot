package com.PlanAceBot.service;

import com.PlanAceBot.model.*;
import com.PlanAceBot.model.User;
import com.PlanAceBot.state.*;
import com.PlanAceBot.config.BotConfig;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.element.AreaBreak;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.awt.*;
import java.io.*;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;

import org.jfree.data.general.DefaultPieDataset;

import java.awt.image.BufferedImage;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.SystemProperties.getUserTimezone;

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
            /payment_details - Отобразить текст о реквизитах оплаты.
            /list_tasks - Показать все задачи пользователя.
            /set_deadline_task - Установить дедлайн для задачи.
            /create_reminder - Создание нового напоминания.
            /update_reminder - Обновление существующего напоминания.
            /delete_reminder - Удаление напоминания.
            /list_reminders - Показать все напоминания пользователя.
            /add_income - Создание нового дохода.
            /add_expense - Создание нового расхода.
            /update_income - Обновление существующей записи о доходе.
            /update_expense - Обновление существующей записи о расходе.
            /delete_income - Удаление записи о доходе.
            /delete_expense - Удаление записи о расходе.
            /add_budget - Создание нового бюджета.
            /update_budget - Обновление существующей записи о бюджете.
            /delete_budget - Удаление бюджета.
            /get_budget_info - Получить информацию о текущем бюджете.
            /show_analytic - Получить аналитический отчет за период.
            /start_pomodoro - Начать сессию помодоро.
            /start_ninety_thirty - Начать сессию 90/30.
            /continue_pomodoro_now - Продлить сессию помодоро на 5 минут.
            /break_pomodoro_now - Завершить сессию помодоро.
            /continue_ninety_thirty_now - Продлить сессию 90/30 на 10 минут.
            /break_ninety_thirty_now - Завершить сессию 90/30.
            /show_task_commands - Отобразить все команды для взаимодействия с задачами.
            /show_reminder_commands - Отобразить все команды для взаимодействия с напоминаниями.
            /show_finance_commands - Отобразить все команды для взаимодействия с финансами.
            /show_income_commands - Отобразить все команды для взаимодействия с доходами.
            /show_expense_commands - Отобразить все команды для взаимодействия с расходами.
            /show_budget_commands - Отобразить все команды для взаимодействия с бюджетом.
            /show_analytic_commands - Отобразить все команды для аналитики.
            /show_time_management_commands - Отобразить все команды для тайм-менеджмента.
            /show_pomodoro_commands - Отобразить все команды для методики помодоро.
            /show_pomodoro_cmds_int - Отобразить все команды для взаимодействия с сессией помодоро.
            /show_ninety_thirty_commands - Отобразить все команды для методики 90/30.
            /show_90_30_cmds_int - Отобразить все команды для взаимодействия с сессией 90/30.
            """;

    private static final String WELCOME_TEXT = "Добро пожаловать! Я бот для управления задачами. :blush:\n" +
            "Используйте команду /help, чтобы увидеть список доступных команд.";

    private static final String WELCOME_BACK_TEXT = "С возвращением! :blush:\n" +
            "Используйте команду /help, чтобы увидеть список доступных команд.";

    static final String ERROR_TEXT = "Error occurred: ";

    private static final String COMMAND_START = "/start";
    private static final String COMMAND_CREATE_TASK = "/create_task";
    private static final String COMMAND_UPDATE_TASK = "/update_task";
    private static final String COMMAND_DELETE_TASK = "/delete_task";
    private static final String COMMAND_CHANGE_STATUS_TASK = "/change_status_task";
    private static final String COMMAND_HELP = "/help";
    private static final String COMMAND_LIST_TASKS = "/list_tasks";
    private static final String COMMAND_SET_DEADLINE = "/set_deadline_task";

    private static final String COMMAND_CREATE_REMINDER = "/create_reminder";
    private static final String COMMAND_UPDATE_REMINDER = "/update_reminder";
    private static final String COMMAND_DELETE_REMINDER = "/delete_reminder";
    private static final String COMMAND_LIST_REMINDERS = "/list_reminders";

    private static final String COMMAND_SHOW_TASK_COMMANDS = "/show_task_commands";
    private static final String COMMAND_SHOW_REMINDER_COMMANDS = "/show_reminder_commands";
    private static final String COMMAND_SHOW_FINANCE_COMMANDS = "/show_finance_commands";
    private static final String COMMAND_SHOW_INCOME_COMMANDS = "/show_income_commands";
    private static final String COMMAND_SHOW_EXPENSE_COMMANDS = "/show_expense_commands";
    private static final String COMMAND_SHOW_BUDGET_COMMANDS = "/show_budget_commands";
    private static final String COMMAND_SHOW_ANALYTIC_COMMANDS = "/show_analytic_commands";
    private static final String COMMAND_SHOW_TIME_MANAGEMENT_COMMANDS = "/show_time_management_commands";
    private static final String COMMAND_SHOW_POMODORO_COMMANDS = "/show_pomodoro_commands";
    private static final String COMMAND_SHOW_POMODORO_COMMANDS_FOR_INTERACTION = "/show_pomodoro_cmds_int";
    private static final String COMMAND_SHOW_NINETY_THIRTY_COMMANDS = "/show_ninety_thirty_commands";
    private static final String COMMAND_SHOW_NINETY_THIRTY_COMMANDS_FOR_INTERACTION = "/show_90_30_cmds_int";

    private static final String COMMAND_ADD_INCOME = "/add_income";
    private static final String COMMAND_ADD_EXPENSE = "/add_expense";
    private static final String COMMAND_UPDATE_INCOME = "/update_income";
    private static final String COMMAND_UPDATE_EXPENSE = "/update_expense";
    private static final String COMMAND_DELETE_INCOME = "/delete_income";
    private static final String COMMAND_DELETE_EXPENSE = "/delete_expense";
    private static final String COMMAND_ADD_BUDGET = "/add_budget";
    private static final String COMMAND_UPDATE_BUDGET = "/update_budget";
    private static final String COMMAND_DELETE_BUDGET = "/delete_budget";
    private static final String COMMAND_INFO_ABOUT_BUDGET = "/get_budget_info";
    private static final String COMMAND_LIST_INCOMES = "/list_incomes";
    private static final String COMMAND_LIST_EXPENSES = "/list_expenses";

    private static final String COMMAND_CALC = "/calc";

    private static final String COMMAND_SHOW_ANALYTIC = "/show_analytic";

    private static final String COMMAND_START_POMODORO = "/start_pomodoro";
    private static final String COMMAND_START_NINETY_THIRTY = "/start_ninety_thirty";

    private static final String COMMAND_CONTINUE_POMODORO = "/continue_pomodoro_now";
    private static final String COMMAND_BREAK_POMODORO = "/break_pomodoro_now";
    private static final String COMMAND_CONTINUE_NINETY_THIRTY = "/continue_ninety_thirty_now";
    private static final String COMMAND_BREAK_NINETY_THIRTY = "/break_ninety_thirty_now";

    private static final String COMMAND_PAYMENT_DETAILS = "/payment_details";

    public static final String COMMAND_UPDATE_TIMEZONE = "/update_timezone";

    private static final String BUTTON_TITLE = "🏷️ Название";
    private static final String BUTTON_DESCRIPTION = "📝 Описание";
    private static final String BUTTON_PRIORITY = "⚠️ Приоритет";
    private static final String BUTTON_CANCEL = "🚫 Отмена";
    private static final String BUTTON_CONFIRM = "✅ Да";
    private static final String BUTTON_CANCEL_UPDATE = "❌ Нет";
    private static final String BUTTON_SUBSCRIBE = "🔔 Подписаться";
    private static final String BUTTON_CHECK_SUBSCRIPTION = "🕵️ Проверить подписку";
    private static final String BUTTON_REMIND_AT = "⏰ Время напоминания";
    private static final String BUTTON_MESSAGE = "✉️ Сообщение";
    private static final String CHANNEL_NAME = "development_max";
    private static final String CHANNEL_USERNAME = "@development_max";
    private static final String BUTTON_COMPLETE = "✅ Завершена";
    private static final String BUTTON_CANCEL_CHANGE = "❌ Отмена изменений";
    private static final String BUTTON_CONFIRM_BUDGET_INCOME = "✅ Да, добавить к бюджету";
    private static final String BUTTON_CONFIRM_BUDGET_EXPENSE = "✅ Да, изменить бюджет";
    private static final String BUTTON_CHANGE_5MIN = "🕒 Отложить на 5 минут";
    private static final String BUTTON_CHANGE_1HOUR = "⏰ Отложить на 1 час";
    private static final String BUTTON_CHANGE_1DAY = "📅 Отложить на 1 день";
    private static final String BUTTON_CHANGE_ON_YOUR_TIME = "⏱️ Задать время";
    private static final String BUTTON_OK_AND_DELETE = "✅ Ок!";
    private static final String BUTTON_SALARY = "💵 Заработная плата";
    private static final String BUTTON_PENSION = "💰 Пенсия";
    private static final String BUTTON_SCHOLARSHIP = "🎓 Стипендия";
    private static final String BUTTON_STIPEND = "📜 Пособие";
    private static final String BUTTON_INCOME_FROM_THE_SALE_OF_GOODS = "💸 Доход от продажи товаров";
    private static final String BUTTON_EAT = "🍽️ Еда";
    private static final String BUTTON_TRANSPORT = "🚗 Транспорт";
    private static final String BUTTON_ENTERTAINMENTS = "🎉 Развлечения";
    private static final String BUTTON_UTILITIES = "🏠 Коммунальные услуги";
    private static final String BUTTON_MEDICINE = "⚕️ Медицина";
    private static final String BUTTON_OTHER = "🔧 Другое";
    private static final String BUTTON_SUM = "💲 Сумма";
    private static final String BUTTON_DATE = "📅 Дата";
    private static final String BUTTON_CATEGORY = "📂 Категория";
    private static final String BUTTON_START_DATE = "🏁 Дата начала";
    private static final String BUTTON_END_DATE = "🏁 Дата окончания";
    private static final String BUTTON_WARNING = "⚠️ Предупреждение";

    private static final String BUTTON_SHOW_TASK_TEXT = "\uD83D\uDCCB Задачи";
    private static final String BUTTON_SHOW_REMINDER_TEXT = "\uD83D\uDD14 Напоминания";
    private static final String BUTTON_CREATE_TASK_TEXT = "\uD83D\uDCDD Создать задачу";
    private static final String BUTTON_UPDATE_TASK_TEXT = "\uD83D\uDD8A Обновить задачу";
    private static final String BUTTON_DELETE_TASK_TEXT = "\uD83D\uDDD1 Удалить задачу";
    private static final String BUTTON_CHANGE_STATUS_TASK_TEXT = "\uD83D\uDCDB Изменить статус";
    private static final String BUTTON_LIST_TASKS_TEXT = "\uD83D\uDCCB Список задач";
    private static final String BUTTON_SET_DEADLINE_TEXT = "⏰ Установить дедлайн";
    private static final String BUTTON_MAIN_MENU_TEXT = "\uD83C\uDFE0 Вернуться в главное меню";
    private static final String BUTTON_BACK_TEXT = "◀ Вернуться назад";
    private static final String BUTTON_BACK_TEXT_2 = "\uD83D\uDD19 Вернуться назад";
    private static final String BUTTON_FINANCE_TEXT = "\uD83D\uDCB0 Финансы";
    private static final String BUTTON_CREATE_REMINDER_TEXT = "\uD83D\uDCDD Создать напоминание";
    private static final String BUTTON_UPDATE_REMINDER_TEXT = "\uD83D\uDD8A Обновить напоминание";
    private static final String BUTTON_DELETE_REMINDER_TEXT = "\uD83D\uDDD1 Удалить напоминание";
    private static final String BUTTON_LIST_REMINDERS_TEXT = "\uD83D\uDCCB Список напоминаний";
    private static final String BUTTON_SHOW_INCOME_COMMANDS_TEXT = "\uD83D\uDCB5 Доход";
    private static final String BUTTON_SHOW_EXPENSE_COMMANDS_TEXT = "\uD83D\uDCB8 Расход";
    private static final String BUTTON_SHOW_BUDGET_COMMANDS_TEXT = "\uD83D\uDCCB Бюджет";
    private static final String BUTTON_CALC_TEXT = "\uD83D\uDEA7 Калькулятор";
    private static final String BUTTON_ADD_INCOME_TEXT = "\uD83D\uDCB0 Добавить доход";
    private static final String BUTTON_UPDATE_INCOME_TEXT = "✏️ Обновить доход";
    private static final String BUTTON_DELETE_INCOME_TEXT = "❌ Удалить доход";
    private static final String BUTTON_ADD_EXPENSE_TEXT = "\uD83D\uDCB8 Добавить расход";
    private static final String BUTTON_UPDATE_EXPENSE_TEXT = "✏️ Обновить расход";
    private static final String BUTTON_DELETE_EXPENSE_TEXT = "❌ Удалить расход";
    private static final String BUTTON_CREATE_BUDGET_TEXT = "\uD83D\uDCDD Создать бюджет";
    private static final String BUTTON_UPDATE_BUDGET_TEXT = "✏️ Обновить бюджет";
    private static final String BUTTON_DELETE_BUDGET_TEXT = "❌ Удалить бюджет";
    private static final String BUTTON_INFO_ABOUT_BUDGET_TEXT = "\uD83D\uDCCB Информация о бюджете";
    private static final String BUTTON_HELP_TEXT = "❓ Помощь";
    private static final String BUTTON_SHOW_ANALYTIC_COMMANDS_TEXT = "\uD83D\uDCCA Аналитика";
    private static final String BUTTON_SHOW_ANALYTIC_TEXT = "📊 Аналитика бюджета";
    private static final String BUTTON_CONTINUE_POMODORO_TEXT = "⏰ Продлить на 5 минут";
    private static final String BUTTON_BREAK_POMODORO_TEXT = "⏹ Завершить помодоро сессию";
    private static final String BUTTON_SHOW_TIME_MANAGEMENT_COMMANDS_TEXT = "\uD83D\uDD50 Тайм-менеджмент";
    private static final String BUTTON_SHOW_POMODORO_COMMANDS_TEXT = "\uD83C\uDF45 Метод помодоро";
    private static final String BUTTON_START_POMODORO_TEXT = "\uD83C\uDF45 Начать метод помодоро";
    private static final String BUTTON_SHOW_POMODORO_COMMANDS_FOR_INTERACTION_TEXT = "\uD83D\uDD27 Команды для работы с помодоро";
    private static final String BUTTON_CONTINUE_NINETY_THIRTY_TEXT = "⏰ Продлить на 10 минут";
    private static final String BUTTON_BREAK_NINETY_THIRTY_TEXT = "\uD83D\uDED1 Завершить сессию 90 на 30";
    private static final String BUTTON_SHOW_NINETY_THIRTY_COMMANDS_TEXT = "\uD83C\uDFC5 Метод 90 на 30";
    private static final String BUTTON_START_NINETY_THIRTY_TEXT = "\uD83C\uDFC5 Начать сессию 90 на 30";
    private static final String BUTTON_SHOW_NINETY_THIRTY_COMMANDS_FOR_INTERACTION_TEXT = "🔧 Команды для работы с 90 на 30";
    private static final String BUTTON_LIST_INCOMES_TEXT = "\uD83D\uDCB6 Список доходов";
    private static final String BUTTON_LIST_EXPENSES_TEXT = "📉 Список расходов";
    private static final String BUTTON_DONATE_TEXT = "💸 Пожертвование";

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
    private final Map<String, BudgetCreationState> budgetCreationStates = new HashMap<>();
    private final Map<String, BudgetUpdateState> budgetUpdateStates = new HashMap<>();
    private final Map<String, Long> budgetDeletionStates = new HashMap<>();

    private final Map<String, Boolean> calcStates = new HashMap<>();

    private final Map<String, ReportCreationState> reportCreationStates = new HashMap<>();

    private final Map<String, Boolean> timezoneAwaitingUsers = new HashMap<>();

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

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private PomodoroService pomodoroService;

    @Autowired
    private NinetyThirtyService ninetyThirtyService;

    public TelegramBot(BotConfig config) {
        this.botConfig = config;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand(COMMAND_START, "Регистрация пользователя и приветственное сообщение"));
        listofCommands.add(new BotCommand(COMMAND_CALC, "Калькулятор. Введите математическое выражение после команды"));
        listofCommands.add(new BotCommand(COMMAND_CREATE_TASK, "Создание новой задачи"));
        listofCommands.add(new BotCommand(COMMAND_UPDATE_TASK, "Обновление существующей задачи"));
        listofCommands.add(new BotCommand(COMMAND_DELETE_TASK, "Удаление задачи"));
        listofCommands.add(new BotCommand(COMMAND_CHANGE_STATUS_TASK, "Смена статуса задачи"));
        listofCommands.add(new BotCommand(COMMAND_HELP, "Показать инструкцию по командам"));
        listofCommands.add(new BotCommand(COMMAND_PAYMENT_DETAILS, "Отобразить текст о реквизитах оплаты"));
        listofCommands.add(new BotCommand(COMMAND_LIST_TASKS, "Показать все задачи пользователя"));
        listofCommands.add(new BotCommand(COMMAND_SET_DEADLINE, "Установить дедлайн для задачи"));
        listofCommands.add(new BotCommand(COMMAND_CREATE_REMINDER, "Создание нового напоминания"));
        listofCommands.add(new BotCommand(COMMAND_UPDATE_REMINDER, "Обновление существующего напоминания"));
        listofCommands.add(new BotCommand(COMMAND_DELETE_REMINDER, "Удаление напоминания"));
        listofCommands.add(new BotCommand(COMMAND_LIST_REMINDERS, "Показать все напоминания пользователя"));
        listofCommands.add(new BotCommand(COMMAND_ADD_INCOME, "Создание нового дохода"));
        listofCommands.add(new BotCommand(COMMAND_ADD_EXPENSE, "Создание нового расхода"));
        listofCommands.add(new BotCommand(COMMAND_UPDATE_INCOME, "Обновление существующей записи о доходе"));
        listofCommands.add(new BotCommand(COMMAND_UPDATE_EXPENSE, "Обновление существующей записи о расходе"));
        listofCommands.add(new BotCommand(COMMAND_DELETE_INCOME, "Удаление записи о доходе"));
        listofCommands.add(new BotCommand(COMMAND_DELETE_EXPENSE, "Удаление записи о расходе"));
        listofCommands.add(new BotCommand(COMMAND_ADD_BUDGET, "Создание нового бюджета"));
        listofCommands.add(new BotCommand(COMMAND_UPDATE_BUDGET, "Обновление существующей записи о бюджете"));
        listofCommands.add(new BotCommand(COMMAND_DELETE_BUDGET, "Удаление записи о бюджете"));
        listofCommands.add(new BotCommand(COMMAND_INFO_ABOUT_BUDGET, "Получить информацию о текущем бюджете"));
        listofCommands.add(new BotCommand(COMMAND_START_POMODORO, "Начать сессию помодоро"));
        listofCommands.add(new BotCommand(COMMAND_START_NINETY_THIRTY, "Начать сессию 90/30"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_ANALYTIC, "Получить аналитический отчет за период"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_TASK_COMMANDS, "Отобразить все команды для взаимодействия с задачами"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_REMINDER_COMMANDS, "Отобразить все команды для взаимодействия с напоминаниями"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_FINANCE_COMMANDS, "Отобразить все команды для взаимодействия с финансами"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_INCOME_COMMANDS, "Отобразить все команды для взаимодействия с доходами"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_EXPENSE_COMMANDS, "Отобразить все команды для взаимодействия с расходами"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_BUDGET_COMMANDS, "Отобразить все команды для взаимодействия с бюджетом"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_ANALYTIC_COMMANDS, "Отобразить все команды для аналитики"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_TIME_MANAGEMENT_COMMANDS, "Отобразить все команды для тайм-менеджмента"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_POMODORO_COMMANDS, "Отобразить все команды для методики помодоро"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_POMODORO_COMMANDS_FOR_INTERACTION, "Отобразить все команды для взаимодействия с сессией помодоро"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_NINETY_THIRTY_COMMANDS, "Отобразить все команды для методики 90/30"));
        listofCommands.add(new BotCommand(COMMAND_SHOW_NINETY_THIRTY_COMMANDS_FOR_INTERACTION, "Отобразить все команды для взаимодействия с сессией 90/30"));

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

            if (isAwaitingTimezone(chatId)) {
                boolean isRegistration = timezoneAwaitingUsers.get(chatId);
                saveUserTimezone(chatId, messageText);
                if (isRegistration) {
                    sendWelcomeMessage(chatId);
                }
                return;
            }

            command = switch (messageText) {
                case BUTTON_SHOW_TASK_TEXT -> COMMAND_SHOW_TASK_COMMANDS;
                case BUTTON_SHOW_REMINDER_TEXT -> COMMAND_SHOW_REMINDER_COMMANDS;
                case BUTTON_CREATE_TASK_TEXT -> COMMAND_CREATE_TASK;
                case BUTTON_UPDATE_TASK_TEXT -> COMMAND_UPDATE_TASK;
                case BUTTON_DELETE_TASK_TEXT -> COMMAND_DELETE_TASK;
                case BUTTON_CHANGE_STATUS_TASK_TEXT -> COMMAND_CHANGE_STATUS_TASK;
                case BUTTON_LIST_TASKS_TEXT -> COMMAND_LIST_TASKS;
                case BUTTON_SET_DEADLINE_TEXT -> COMMAND_SET_DEADLINE;
                case BUTTON_MAIN_MENU_TEXT -> COMMAND_START;
                case BUTTON_BACK_TEXT, BUTTON_FINANCE_TEXT -> COMMAND_SHOW_FINANCE_COMMANDS;
                case BUTTON_CREATE_REMINDER_TEXT -> COMMAND_CREATE_REMINDER;
                case BUTTON_UPDATE_REMINDER_TEXT -> COMMAND_UPDATE_REMINDER;
                case BUTTON_DELETE_REMINDER_TEXT -> COMMAND_DELETE_REMINDER;
                case BUTTON_LIST_REMINDERS_TEXT -> COMMAND_LIST_REMINDERS;
                case BUTTON_SHOW_INCOME_COMMANDS_TEXT -> COMMAND_SHOW_INCOME_COMMANDS;
                case BUTTON_SHOW_EXPENSE_COMMANDS_TEXT -> COMMAND_SHOW_EXPENSE_COMMANDS;
                case BUTTON_SHOW_BUDGET_COMMANDS_TEXT -> COMMAND_SHOW_BUDGET_COMMANDS;
                case BUTTON_CALC_TEXT -> COMMAND_CALC;
                case BUTTON_ADD_INCOME_TEXT -> COMMAND_ADD_INCOME;
                case BUTTON_UPDATE_INCOME_TEXT -> COMMAND_UPDATE_INCOME;
                case BUTTON_DELETE_INCOME_TEXT -> COMMAND_DELETE_INCOME;
                case BUTTON_ADD_EXPENSE_TEXT -> COMMAND_ADD_EXPENSE;
                case BUTTON_UPDATE_EXPENSE_TEXT -> COMMAND_UPDATE_EXPENSE;
                case BUTTON_DELETE_EXPENSE_TEXT -> COMMAND_DELETE_EXPENSE;
                case BUTTON_CREATE_BUDGET_TEXT -> COMMAND_ADD_BUDGET;
                case BUTTON_UPDATE_BUDGET_TEXT -> COMMAND_UPDATE_BUDGET;
                case BUTTON_DELETE_BUDGET_TEXT -> COMMAND_DELETE_BUDGET;
                case BUTTON_INFO_ABOUT_BUDGET_TEXT -> COMMAND_INFO_ABOUT_BUDGET;
                case BUTTON_HELP_TEXT -> COMMAND_HELP;
                case BUTTON_SHOW_ANALYTIC_COMMANDS_TEXT -> COMMAND_SHOW_ANALYTIC_COMMANDS;
                case BUTTON_SHOW_ANALYTIC_TEXT -> COMMAND_SHOW_ANALYTIC;
                case BUTTON_CONTINUE_POMODORO_TEXT -> COMMAND_CONTINUE_POMODORO;
                case BUTTON_BREAK_POMODORO_TEXT -> COMMAND_BREAK_POMODORO;
                case BUTTON_SHOW_TIME_MANAGEMENT_COMMANDS_TEXT, BUTTON_BACK_TEXT_2 -> COMMAND_SHOW_TIME_MANAGEMENT_COMMANDS;
                case BUTTON_SHOW_POMODORO_COMMANDS_TEXT -> COMMAND_SHOW_POMODORO_COMMANDS;
                case BUTTON_START_POMODORO_TEXT -> COMMAND_START_POMODORO;
                case BUTTON_SHOW_POMODORO_COMMANDS_FOR_INTERACTION_TEXT -> COMMAND_SHOW_POMODORO_COMMANDS_FOR_INTERACTION;
                case BUTTON_CONTINUE_NINETY_THIRTY_TEXT -> COMMAND_CONTINUE_NINETY_THIRTY;
                case BUTTON_BREAK_NINETY_THIRTY_TEXT -> COMMAND_BREAK_NINETY_THIRTY;
                case BUTTON_SHOW_NINETY_THIRTY_COMMANDS_TEXT -> COMMAND_SHOW_NINETY_THIRTY_COMMANDS;
                case BUTTON_START_NINETY_THIRTY_TEXT -> COMMAND_START_NINETY_THIRTY;
                case BUTTON_SHOW_NINETY_THIRTY_COMMANDS_FOR_INTERACTION_TEXT -> COMMAND_SHOW_NINETY_THIRTY_COMMANDS_FOR_INTERACTION;
                case BUTTON_LIST_INCOMES_TEXT -> COMMAND_LIST_INCOMES;
                case BUTTON_LIST_EXPENSES_TEXT -> COMMAND_LIST_EXPENSES;
                case BUTTON_DONATE_TEXT -> COMMAND_PAYMENT_DETAILS;
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
            } else if (budgetCreationStates.containsKey(chatId)) {
                processBudgetCreation(chatId, messageText);
            } else if (budgetUpdateStates.containsKey(chatId)) {
                processFieldAndValueForBudget(chatId, messageText);
            } else if (budgetDeletionStates.containsKey(chatId)) {
                sendDeleteBudgetConfirmationMessage(chatId, budgetDeletionStates.get(chatId));
            } else if (reportCreationStates.containsKey(chatId)) {
                processReportCreation(chatId, messageText);
            } else {
                switch (command) {
                    case COMMAND_START:
                        registerUserAndSendWelcomeMessage(chatId, update.getMessage(), !messageText.equals(BUTTON_MAIN_MENU_TEXT));
                        break;

                    case COMMAND_CALC:
                        handleCalculateCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_CREATE_TASK:
                        handleTaskCreationCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_UPDATE_TASK:
                        handleUpdateCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_DELETE_TASK:
                        handleDeleteCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_CHANGE_STATUS_TASK:
                        handleChangeStatusCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_HELP:
                        sendHelpMessage(chatId, parts, messageText);
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
                        showTaskCommandsKeyboard(chatId);
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

                    case COMMAND_ADD_BUDGET:
                        handleBudgetCreationCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_UPDATE_BUDGET:
                        handleUpdateBudgetCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_DELETE_BUDGET:
                        handleDeleteBudgetCommand(parts, chatId, messageText);
                        break;

                    case COMMAND_INFO_ABOUT_BUDGET:
                        sendCurrentBudgetInfo(chatId, parts, messageText);
                        break;

                    case COMMAND_SHOW_FINANCE_COMMANDS:
                        showFinanceCommandsKeyboard(chatId);
                        break;

                    case COMMAND_SHOW_INCOME_COMMANDS:
                        showIncomeCommandsKeyboard(chatId);
                        break;

                    case COMMAND_SHOW_EXPENSE_COMMANDS:
                        showExpenseCommandsKeyboard(chatId);
                        break;

                    case COMMAND_SHOW_BUDGET_COMMANDS:
                        showBudgetCommandsKeyboard(chatId);
                        break;

                    case COMMAND_SHOW_ANALYTIC:
                        handleReportCreationCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_SHOW_ANALYTIC_COMMANDS:
                        showBudgetAnalyticsKeyboard(chatId);
                        break;

                    case COMMAND_START_POMODORO:
                        handlePomodoroCommands(chatId, parts, messageText);
                        break;

                    case COMMAND_CONTINUE_POMODORO:
                        extendWorkInterval(chatId);
                        break;

                    case COMMAND_BREAK_POMODORO:
                        endPomodoroSession(chatId);
                        break;

                    case COMMAND_SHOW_TIME_MANAGEMENT_COMMANDS:
                        showTimeManagementMenuKeyboard(chatId);
                        break;

                    case COMMAND_SHOW_POMODORO_COMMANDS:
                        showPomodoroManagementKeyboard(chatId);
                        break;

                    case COMMAND_SHOW_POMODORO_COMMANDS_FOR_INTERACTION:
                        showPomodoroCommandsKeyboard(chatId);
                        break;

                    case COMMAND_START_NINETY_THIRTY:
                        handleNinetyThirtyCommands(chatId, parts, messageText);
                        break;

                    case COMMAND_CONTINUE_NINETY_THIRTY:
                        extendNinetyThirtyWorkInterval(chatId);
                        break;

                    case COMMAND_BREAK_NINETY_THIRTY:
                        endNinetyThirtySession(chatId);
                        break;

                    case COMMAND_SHOW_NINETY_THIRTY_COMMANDS:
                        showNinetyThirtyManagementKeyboard(chatId);
                        break;

                    case COMMAND_SHOW_NINETY_THIRTY_COMMANDS_FOR_INTERACTION:
                        showNinetyThirtyCommandsKeyboard(chatId);
                        break;

                    case COMMAND_LIST_INCOMES:
                        handleListIncomesCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_LIST_EXPENSES:
                        handleListExpensesCommand(chatId, parts, messageText);
                        break;

                    case COMMAND_PAYMENT_DETAILS:
                        sendPaymentDetails(chatId);
                        break;

                    case COMMAND_UPDATE_TIMEZONE:
                        sendTimezoneRequestMessage(chatId, false);
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

    private boolean isAwaitingTimezone(String chatId) {
        return timezoneAwaitingUsers.containsKey(chatId);
    }

    private void saveUserTimezone(String chatId, String timezone) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user != null) {
            user.setTimezone(timezone);
            userService.save(user);
            sendMessage(chatId, "Ваш часовой пояс был обновлен до " + timezone + ".");
            timezoneAwaitingUsers.remove(chatId);
        }
    }

    private void sendPaymentDetails(String chatId) {
        User user = userService.getUserByChatId(chatId);
        String userId = user.getChatId().toString();

        StringBuilder message = new StringBuilder();
        message.append("💳 Способ оплаты: Оплатить картой (любой банк)\n");
        message.append("🌟 Пожертвование на развитие проектов 🌟\n");
        message.append("🆔 Ваш ID: ").append(userId).append("\n\n");
        message.append("📋 Реквизиты для оплаты:\n\n");
        message.append("💳 Данные для оплаты:\n");
        message.append("   💳 Номер карты: 👇\n");
        message.append("      <code>4100117685810867</code>\n\n");
        message.append("📜 Пожалуйста, убедитесь в корректности введённых данных.\n");
        message.append("__________________________\n\n");
        message.append("Вы платите физическому лицу. 💼 Деньги поступят на счёт получателя.\n");
        message.append("🚀 Ваш вклад в развитие проектов неоценим для нас! Спасибо!");

        sendMessageWithHTML(chatId, message.toString(), true);
    }

    private void sendMessageWithHTML(String chatId, String text, boolean enableHtml) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableHtml(enableHtml);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handlePomodoroCommands(String chatId, String[] parts, String messageText) {
        if (parts.length == 1 || messageText.equals(BUTTON_START_POMODORO_TEXT)) {
            startPomodoroSession(chatId);
        } else {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /start_pomodoro без параметров.");
        }
    }

    public void startPomodoroSession(String chatId) {
        User user = userService.getUserByChatId(chatId);

        Pomodoro activePomodoro = pomodoroService.getActivePomodoroSessionByUserId(user);
        if (activePomodoro != null) {
            sendMessage(chatId, "⚠️ У вас уже есть активная помодоро-сессия.\n" +
                    "Завершите её перед началом новой.");
            return;
        }

        Pomodoro pomodoro = new Pomodoro();
        pomodoro.setUser(user);

        Timestamp startTime = new Timestamp(System.currentTimeMillis());
        pomodoro.setStartTime(startTime);

        Timestamp endTime = new Timestamp(startTime.getTime() + 25 * 60 * 1000);
        pomodoro.setEndTime(endTime);

        pomodoro.setIntervalType("work");
        pomodoro.setSessionActive(true);
        pomodoroService.savePomodoroSession(pomodoro);

        StringBuilder message = new StringBuilder();
        message.append("🍅 Помодоро сессия начата.\n");
        message.append("Сфокусируйтесь на 25 минут!");

        sendPomodoroMessage(chatId, message.toString(), createPomodoroKeyboard());
    }

    public void extendWorkInterval(String chatId) {
        Pomodoro pomodoro = pomodoroService.getPomodoroSessionByChatId(chatId);
        if (pomodoro != null) {
            Timestamp currentEndTime = pomodoro.getEndTime();
            Timestamp newEndTime = new Timestamp(currentEndTime.getTime() + 5 * 60 * 1000);

            pomodoro.setEndTime(newEndTime);
            pomodoroService.savePomodoroSession(pomodoro);

            StringBuilder message = new StringBuilder();
            message.append("⏲️ Интервал продлен на 5 минут!\n");
            message.append("Продолжайте сосредотачиваться!");

            sendPomodoroMessage(chatId, message.toString(), createPomodoroKeyboard());
        } else {
            sendMessage(chatId, "❌ Помодоро-сессия не найдена.");
        }
    }

    public void endPomodoroSession(String chatId) {
        Pomodoro pomodoro = pomodoroService.getPomodoroSessionByChatId(chatId);
        if (pomodoro != null) {
            pomodoro.setSessionActive(false);
            pomodoroService.savePomodoroSession(pomodoro);
            pomodoroService.deletePomodoroSession(pomodoro);

            StringBuilder message = new StringBuilder();
            message.append("🍅 Помодоро сессия завершена!\n");
            message.append("Отличная работа! Возможно, пора сделать перерыв?");

            createMainMenuKeyboard(chatId, message.toString());
        } else {
            sendMessage(chatId, "❌ Помодоро-сессия не найдена.");
        }
    }


    public void sendPomodoroMessage(String chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public ReplyKeyboardMarkup createPomodoroKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(BUTTON_CONTINUE_POMODORO_TEXT);
        row1.add(BUTTON_BREAK_POMODORO_TEXT);
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(BUTTON_MAIN_MENU_TEXT);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        return keyboardMarkup;
    }

    private void handleNinetyThirtyCommands(String chatId, String[] parts, String messageText) {
        if (parts.length == 1 || messageText.equals(BUTTON_START_NINETY_THIRTY_TEXT)) {
            startNinetyThirtySession(chatId);
        } else {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /start_ninety_thirty без параметров.");
        }
    }

    public void startNinetyThirtySession(String chatId) {
        User user = userService.getUserByChatId(chatId);

        NinetyThirty activeSession = ninetyThirtyService.getActiveNinetyThirtySessionByUserId(user);
        if (activeSession != null) {
            sendMessage(chatId, "У вас уже есть активная сессия. Завершите её перед началом новой. 😊");
            return;
        }

        NinetyThirty session = new NinetyThirty();
        session.setUser(user);

        Timestamp startTime = new Timestamp(System.currentTimeMillis());
        session.setStartTime(startTime);

        Timestamp endTime = new Timestamp(startTime.getTime() + 90 * 60 * 1000);
        session.setEndTime(endTime);

        session.setIntervalType("work90");
        session.setSessionActive(true);
        ninetyThirtyService.saveNinetyThirtySession(session);

        sendNinetyThirtyMessage(chatId, "Сессия 90 на 30 начата. Сфокусируйтесь на 90 минут! 💪", createNinetyThirtyKeyboard());
    }

    public void extendNinetyThirtyWorkInterval(String chatId) {
        NinetyThirty session = ninetyThirtyService.getNinetyThirtySessionByChatId(chatId);
        if (session != null) {
            if ("work90".equals(session.getIntervalType()) || "rest30".equals(session.getIntervalType())) {
                Timestamp currentEndTime = session.getEndTime();
                Timestamp newEndTime = new Timestamp(currentEndTime.getTime() + 10 * 60 * 1000);

                session.setEndTime(newEndTime);
                ninetyThirtyService.saveNinetyThirtySession(session);

                sendNinetyThirtyMessage(chatId, "Интервал продлен на 10 минут! ⏳", createNinetyThirtyKeyboard());
            } else {
                sendMessage(chatId, "Текущая сессия не является рабочей сессией 90 на 30. 😕");
            }
        } else {
            sendMessage(chatId, "Сессия 90 на 30 не найдена. 🕵️‍♂️");
        }
    }

    public void endNinetyThirtySession(String chatId) {
        NinetyThirty session = ninetyThirtyService.getNinetyThirtySessionByChatId(chatId);
        if (session != null) {
            session.setSessionActive(false);
            ninetyThirtyService.saveNinetyThirtySession(session);

            ninetyThirtyService.deleteNinetyThirtySession(session);

            createMainMenuKeyboard(chatId, "Сессия 90 на 30 завершена. Отличная работа! 🎉");
        } else {
            sendMessage(chatId, "Сессия 90 на 30 не найдена. 🕵️‍♂️");
        }
    }

    public void sendNinetyThirtyMessage(String chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public ReplyKeyboardMarkup createNinetyThirtyKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(BUTTON_CONTINUE_NINETY_THIRTY_TEXT);
        row1.add(BUTTON_BREAK_NINETY_THIRTY_TEXT);
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(BUTTON_MAIN_MENU_TEXT);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        return keyboardMarkup;
    }

    private void showTaskCommandsKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createKeyboardRow(BUTTON_CREATE_TASK_TEXT, BUTTON_UPDATE_TASK_TEXT, BUTTON_DELETE_TASK_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_CHANGE_STATUS_TASK_TEXT, BUTTON_LIST_TASKS_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_SET_DEADLINE_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_MAIN_MENU_TEXT));

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите команду: 📋")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке клавиатуры с командами: {}", e.getMessage());
        }
    }

    private void showReminderCommandsKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createKeyboardRow(BUTTON_CREATE_REMINDER_TEXT, BUTTON_UPDATE_REMINDER_TEXT, BUTTON_DELETE_REMINDER_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_LIST_REMINDERS_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_MAIN_MENU_TEXT));

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите команду для управления напоминаниями: ⏰")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке клавиатуры с командами для напоминаний: {}", e.getMessage());
        }
    }

    private void showFinanceCommandsKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createKeyboardRow(BUTTON_SHOW_INCOME_COMMANDS_TEXT, BUTTON_SHOW_EXPENSE_COMMANDS_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_SHOW_BUDGET_COMMANDS_TEXT, BUTTON_CALC_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_MAIN_MENU_TEXT));

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите раздел финансов: 💰")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке клавиатуры с командами для финансов: {}", e.getMessage());
        }
    }

    private void showIncomeCommandsKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createKeyboardRow(BUTTON_ADD_INCOME_TEXT, BUTTON_UPDATE_INCOME_TEXT, BUTTON_DELETE_INCOME_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_LIST_INCOMES_TEXT, BUTTON_BACK_TEXT));

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите действие с доходом: 💸")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке клавиатуры с командами для доходов: {}", e.getMessage());
        }
    }

    private void showExpenseCommandsKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createKeyboardRow(BUTTON_ADD_EXPENSE_TEXT, BUTTON_UPDATE_EXPENSE_TEXT, BUTTON_DELETE_EXPENSE_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_LIST_EXPENSES_TEXT, BUTTON_BACK_TEXT));

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите действие с расходом: 💸")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке клавиатуры с командами для расходов: {}", e.getMessage());
        }
    }

    private void showBudgetCommandsKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createKeyboardRow(BUTTON_CREATE_BUDGET_TEXT, BUTTON_UPDATE_BUDGET_TEXT, BUTTON_DELETE_BUDGET_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_INFO_ABOUT_BUDGET_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_BACK_TEXT));

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите действие с бюджетом: 💰")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке клавиатуры с командами для бюджета: {}", e.getMessage());
        }
    }

    private void showBudgetAnalyticsKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createKeyboardRow(BUTTON_SHOW_ANALYTIC_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_MAIN_MENU_TEXT));

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите действие для аналитики бюджета: 📊")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке клавиатуры с аналитикой бюджета: {}", e.getMessage());
        }
    }

    private void showTimeManagementMenuKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(BUTTON_SHOW_POMODORO_COMMANDS_TEXT);

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add(BUTTON_SHOW_NINETY_THIRTY_COMMANDS_TEXT);

        KeyboardRow thirdRow = new KeyboardRow();
        thirdRow.add(BUTTON_MAIN_MENU_TEXT);

        keyboard.add(firstRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите действие для управления временем: ⏳")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке клавиатуры меню управления временем: {}", e.getMessage());
        }
    }

    private void showPomodoroManagementKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(BUTTON_START_POMODORO_TEXT);

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add(BUTTON_SHOW_POMODORO_COMMANDS_FOR_INTERACTION_TEXT);

        KeyboardRow thirdRow = new KeyboardRow();
        thirdRow.add(BUTTON_BACK_TEXT_2);

        keyboard.add(firstRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите действие для управления помодоро: \uD83D\uDD52")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке клавиатуры управления помодоро: {}", e.getMessage());
        }
    }

    private void showPomodoroCommandsKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createKeyboardRow(BUTTON_CONTINUE_POMODORO_TEXT, BUTTON_BREAK_POMODORO_TEXT));
        keyboard.add(createKeyboardRow(BUTTON_MAIN_MENU_TEXT));

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите действие с помодоро: 🍅")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке клавиатуры команд помодоро: {}", e.getMessage());
        }
    }

    private void showNinetyThirtyManagementKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(BUTTON_START_NINETY_THIRTY_TEXT);

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add(BUTTON_SHOW_NINETY_THIRTY_COMMANDS_FOR_INTERACTION_TEXT);

        KeyboardRow thirdRow = new KeyboardRow();
        thirdRow.add(BUTTON_BACK_TEXT_2);

        keyboard.add(firstRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите действие: ⏳")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке клавиатуры управления 90 на 30: {}", e.getMessage());
        }
    }

    private void showNinetyThirtyCommandsKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createKeyboardRow(BUTTON_CONTINUE_NINETY_THIRTY_TEXT, BUTTON_BREAK_NINETY_THIRTY_TEXT));

        keyboard.add(createKeyboardRow(BUTTON_BACK_TEXT_2));

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите действие для сессии 90 на 30: ⏳")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending ninety-thirty commands keyboard: {}", e.getMessage());
        }
    }

    private KeyboardRow createKeyboardRow(String... buttons) {
        KeyboardRow row = new KeyboardRow();
        for (String button : buttons) {
            row.add(button);
        }
        return row;
    }

    private void handleCalculateCommand(String chatId, String[] parts, String messageText) {
        if (parts.length == 1 || messageText.equals(BUTTON_CALC_TEXT)) {
            sendMessage(chatId, "Пожалуйста, введите математическое выражение для вычисления: 🧮");
            calcStates.put(chatId, true);
        } else {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /calc без параметров.");
        }
    }

    private void handleCalculateExpression(String chatId, String expression) {
        try {
            Expression e = new ExpressionBuilder(expression).build();
            double result = e.evaluate();

            if (result == Math.floor(result)) {
                sendMessage(chatId, "Результат: " + (int) result + " 🎲");
            } else {
                sendMessage(chatId, "Результат: " + result + " 🎲");
            }

        } catch (Exception ex) {
            sendMessage(chatId, "Ошибка в выражении. Пожалуйста, проверьте правильность ввода. ❌");
        }
    }

    private void sendHelpMessage(String chatId, String[] parts, String messageText) {
        if (parts.length > 1 && !(messageText.equals(BUTTON_HELP_TEXT))) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте команду /help только без параметров.");
            return;
        }

        String helpMessage = EmojiParser.parseToUnicode(HELP_TEXT);
        sendMessage(chatId, helpMessage);
    }

    private void sendSubscribeMessage(String chatId) {
        String subscribeMessage = "Подпишитесь на наш канал и затем нажмите кнопку \"Проверить подписку\", чтобы продолжить использование бота. 📢";
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

    private void registerUserAndSendWelcomeMessage(String chatId, Message msg, boolean flag) {
        boolean isNewUser = false;

        if (!userService.existByChatId(Long.parseLong(chatId))) {
            var chat = msg.getChat();

            User currentUser = new User(chatId);
            currentUser.setChatId(Long.parseLong(chatId));
            currentUser.setFirstName(chat.getFirstName());
            currentUser.setLastName(chat.getLastName());
            currentUser.setUsername(chat.getUserName());
            currentUser.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userService.save(currentUser);
            isNewUser = true;
        }

        if (isNewUser && flag) {
            sendTimezoneRequestMessage(chatId, true);
        } else if (!isNewUser && flag) {
            sendWelcomeBackMessage(chatId);
        } else {
            createStartKeyboardForBack(chatId, "Вы вернулись в главное меню 😊");
        }
    }

    private void sendTimezoneRequestMessage(String chatId, boolean isRegistration) {
        String message = "Пожалуйста, введите ваш часовой пояс (Например, 'Europe/Moscow' or 'UTC+3'):";
        sendMessage(chatId, message);

        timezoneAwaitingUsers.put(chatId, isRegistration);
    }

    private void sendWelcomeMessage(String chatId) {
        String welcomeMessage = EmojiParser.parseToUnicode(WELCOME_TEXT);

        createStartKeyboardForWelcome(chatId, welcomeMessage);

    }

    private void sendWelcomeBackMessage(String chatId) {
        String welcomeBackMessage = EmojiParser.parseToUnicode(WELCOME_BACK_TEXT);

        createStartKeyboardForWelcomeBack(chatId, welcomeBackMessage);
    }

    private void createMainMenuKeyboard(String chatId, String backMenu) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(BUTTON_SHOW_TASK_TEXT);
        firstRow.add(BUTTON_SHOW_REMINDER_TEXT);

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add(BUTTON_FINANCE_TEXT);
        secondRow.add(BUTTON_SHOW_ANALYTIC_COMMANDS_TEXT);

        KeyboardRow thirdRow = new KeyboardRow();
        thirdRow.add(BUTTON_SHOW_TIME_MANAGEMENT_COMMANDS_TEXT);

        KeyboardRow fourthRow = new KeyboardRow();
        fourthRow.add(BUTTON_DONATE_TEXT);
        fourthRow.add(BUTTON_HELP_TEXT);

        keyboard.add(firstRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);
        keyboard.add(fourthRow);

        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(backMenu)
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending welcome message: {}", e.getMessage());
        }
    }

    private void createStartKeyboardForBack(String chatId, String backMessage) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(BUTTON_SHOW_TASK_TEXT);
        firstRow.add(BUTTON_SHOW_REMINDER_TEXT);

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add(BUTTON_FINANCE_TEXT);
        secondRow.add(BUTTON_SHOW_ANALYTIC_COMMANDS_TEXT);

        KeyboardRow thirdRow = new KeyboardRow();
        thirdRow.add(BUTTON_SHOW_TIME_MANAGEMENT_COMMANDS_TEXT);

        KeyboardRow fourthRow = new KeyboardRow();
        fourthRow.add(BUTTON_DONATE_TEXT);
        fourthRow.add(BUTTON_HELP_TEXT);

        keyboard.add(firstRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);
        keyboard.add(fourthRow);

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

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(BUTTON_SHOW_TASK_TEXT);
        firstRow.add(BUTTON_SHOW_REMINDER_TEXT);

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add(BUTTON_FINANCE_TEXT);
        secondRow.add(BUTTON_SHOW_ANALYTIC_COMMANDS_TEXT);

        KeyboardRow thirdRow = new KeyboardRow();
        thirdRow.add(BUTTON_SHOW_TIME_MANAGEMENT_COMMANDS_TEXT);

        KeyboardRow fourthRow = new KeyboardRow();
        fourthRow.add(BUTTON_DONATE_TEXT);
        fourthRow.add(BUTTON_HELP_TEXT);

        keyboard.add(firstRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);
        keyboard.add(fourthRow);

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

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(BUTTON_SHOW_TASK_TEXT);
        firstRow.add(BUTTON_SHOW_REMINDER_TEXT);

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add(BUTTON_FINANCE_TEXT);
        secondRow.add(BUTTON_SHOW_ANALYTIC_COMMANDS_TEXT);

        KeyboardRow thirdRow = new KeyboardRow();
        thirdRow.add(BUTTON_SHOW_TIME_MANAGEMENT_COMMANDS_TEXT);

        KeyboardRow fourthRow = new KeyboardRow();
        fourthRow.add(BUTTON_DONATE_TEXT);
        fourthRow.add(BUTTON_HELP_TEXT);

        keyboard.add(firstRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);
        keyboard.add(fourthRow);

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
        if (parts.length == 1 || messageText.equals(BUTTON_CREATE_TASK_TEXT)) {
            startTaskCreation(chatId);
        } else {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /create_task без параметров.");
        }
    }


    private void startTaskCreation(String chatId) {
        if (getUserTaskCount(chatId) > 20) {
            sendMessage(chatId, "Вы достигли максимального количества задач (20) 😕. Удалите существующие задачи перед созданием новых.");
            return;
        }

        taskCreationStates.put(chatId, new TaskCreationState());
        sendMessage(chatId, "Введите название задачи: ✍️");
    }

    private void processTaskCreation(String chatId, String messageText) {
        TaskCreationState currentState = taskCreationStates.get(chatId);

        if (currentState.getState() == TaskState.ENTER_TITLE) {
            currentState.setTitle(messageText);
            currentState.setState(TaskState.ENTER_DESCRIPTION);
            sendMessage(chatId, "Введите описание задачи для '" + messageText + "': ✏️");
        } else if (currentState.getState() == TaskState.ENTER_DESCRIPTION) {
            currentState.setDescription(messageText);
            currentState.setState(TaskState.ENTER_PRIORITY);
            sendMessage(chatId, "Введите приоритет задачи (1-5): 🌟");
        } else if (currentState.getState() == TaskState.ENTER_PRIORITY) {
            try {
                int priority = Integer.parseInt(messageText);
                if (priority < 1 || priority > 5) {
                    sendMessage(chatId, "Приоритет должен быть в диапазоне от 1 до 5. Пожалуйста, введите заново: ❗");
                    return;
                }
                currentState.setPriority(priority);

                createTask(currentState.getTitle(), currentState.getDescription(), currentState.getPriority(), chatId);
                taskCreationStates.remove(chatId);

                sendMessage(chatId, "Задача '" + currentState.getTitle() + "' с приоритетом " + priority + " создана! 🎉");
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Пожалуйста, введите числовое значение для приоритета (1-5): ❓");
            }
        }
    }

    private void createTask(String title, String description, int priority, String chatId) {
        if (getUserTaskCount(chatId) > 20) {
            sendMessage(chatId, "Вы достигли максимального количества задач (20) 😕. Удалите существующие задачи перед созданием новых.");
            return;
        }

        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "Пользователь не зарегистрирован. Используйте /start для регистрации. 🚫");
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
        sendMessage(chatId, "Неизвестная команда. Используйте /help, чтобы увидеть доступные команды. ❓");
    }

    private void handleUpdateCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals(BUTTON_UPDATE_TASK_TEXT))) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте команду /update_task только без параметров.");
            return;
        }

        List<Task> tasks = taskService.findTasksByUserId(Long.parseLong(chatId));

        if (tasks.isEmpty()) {
            sendMessage(chatId, "У вас пока нет задач для обновления. 😕");
            return;
        }

        InlineKeyboardMarkup markup = createTasksMarkup(tasks);

        SendMessage message = createMessage(chatId, "Выберите задачу для обновления: 📝", markup);

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
            sendMessage(chatId, "Ошибка обновления задачи. ❌");
            return;
        }

        int taskId = currentState.getTaskId();

        Task task = taskService.findById(taskId);
        if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "Задача с указанным номером не найдена или не принадлежит вам. 🚫");
            return;
        }

        String currentTitle = task.getTitle();
        String currentDescription = task.getDescription();
        int currentPriority = task.getPriority();
        LocalDateTime creationTimestamp = task.getCreationTimestamp().toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String selectionMessage = "Выберите, что вы хотите обновить для задачи:\n";
        selectionMessage += "📝 Текущее название: " + currentTitle + "\n";
        selectionMessage += "📋 Текущее описание: " + currentDescription + "\n";
        selectionMessage += "⭐ Текущий приоритет: " + currentPriority + "\n";
        selectionMessage += "🕒 Дата создания: " + creationTimestamp.format(formatter) + "\n";

        LocalDateTime deadline = task.getDeadline();
        if (deadline != null) {
            selectionMessage += "⏰ Дедлайн: " + deadline.format(formatter) + "\n";
        }

        InlineKeyboardMarkup markup = createUpdateMarkup();

        SendMessage message = createMessage(chatId, selectionMessage, markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения выбора поля: {}", e.getMessage());
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
            sendMessage(chatId, "Ошибка при обновлении задачи. ❌");
            return;
        }

        int taskId = currentState.getTaskId();
        String fieldToUpdate = currentState.getFieldToUpdate();

        Task task = taskService.findById(taskId);
        if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "Задача с указанным номером не найдена или не принадлежит вам. 🚫");
            taskUpdateStates.remove(chatId);
            return;
        }

        if (fieldToUpdate.equals("priority")) {
            try {
                int priority = Integer.parseInt(messageText);
                if (priority < 1 || priority > 5) {
                    sendMessage(chatId, "Приоритет должен быть в диапазоне от 1 до 5. Пожалуйста, введите заново: ❗");
                    return;
                }
                task.setPriority(priority);
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Пожалуйста, введите числовое значение для приоритета (1-5): ❓");
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
                sendMessage(task.getUser().getChatId().toString(), "Ошибка при обновлении задачи. ❌");
                taskUpdateStates.remove(task.getUser().getChatId().toString());
                return;
        }

        taskService.save(task);
    }

    private void sendConfirmationMessage(String chatId, Task task) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("Изменения сохранены:\n");
        confirmationMessage.append("📝 Название: ").append(task.getTitle()).append("\n");
        confirmationMessage.append("📋 Описание: ").append(task.getDescription()).append("\n");
        confirmationMessage.append("⭐ Приоритет: ").append(task.getPriority()).append("\n");

        confirmationMessage.append("\n🕒 Дата создания: ").append(task.getCreationTimestamp().toLocalDateTime().format(formatter));

        LocalDateTime deadline = task.getDeadline();
        if (deadline != null) {
            confirmationMessage.append("\n⏰ Дедлайн: ").append(deadline.format(formatter));
        }

        confirmationMessage.append("\n\nПодтвердить изменения?");

        InlineKeyboardMarkup markup = createConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения подтверждения: {}", e.getMessage());
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
        sendMessage(chatId, "Изменения отменены. 🔄");
    }

    private void sendNewValueRequest(String chatId, String field) {
        String messageText;
        switch (field) {
            case "title":
                messageText = "Введите новое название задачи: ✏️";
                break;
            case "description":
                messageText = "Введите новое описание задачи: 📝";
                break;
            case "priority":
                messageText = "Введите новый приоритет задачи (1-5): ⭐";
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
        if (parts.length > 1 && !(messageText.equals(BUTTON_DELETE_TASK_TEXT))) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /delete_task без параметров.");
            return;
        }

        List<Task> tasks = taskService.findTasksByUserId(Long.parseLong(chatId));

        if (tasks.isEmpty()) {
            sendMessage(chatId, "У вас нет задач для удаления. 🚫");
            return;
        }

        InlineKeyboardMarkup markup = createDeleteTaskMarkup(tasks);

        SendMessage message = createMessage(chatId, "\uD83D\uDDD1️ Выберите задачу для удаления:", markup);

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
            sendMessage(chatId, "Задача не найдена. ❌");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("Вы уверены, что хотите удалить следующую задачу?\n\n");
        confirmationMessage.append("📝 Название: ").append(task.getTitle()).append("\n");
        confirmationMessage.append("📋 Описание: ").append(task.getDescription()).append("\n");
        confirmationMessage.append("⭐ Приоритет: ").append(task.getPriority()).append("\n");
        confirmationMessage.append("🕒 Дата создания: ").append(task.getCreationTimestamp().toLocalDateTime().format(formatter)).append("\n");

        LocalDateTime deadline = task.getDeadline();
        if (deadline != null) {
            confirmationMessage.append("⏰ Дедлайн: ").append(deadline.format(formatter)).append("\n");
        }

        InlineKeyboardMarkup markup = createDeleteConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения подтверждения удаления: {}", e.getMessage());
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
        sendMessage(chatId, "Задача удалена. 🗑️");
    }

    private void cancelDelete(String chatId) {
        taskDeletionStates.remove(chatId);
        sendMessage(chatId, "Удаление отменено. 🚫");
    }

    private void handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String chatId = callbackQuery.getMessage().getChatId().toString();
        String data = callbackQuery.getData();
        ExpenseCreationState expenseState = expenseCreationStates.get(chatId);
        ExpenseUpdateState expState = expenseUpdateStates.get(chatId);

        if ("check_subscription".equals(data)) {
            if (isUserSubscribed(chatId)) {
                registerUserAndSendWelcomeMessage(chatId, (Message) callbackQuery.getMessage(),true);
            } else {
                sendMessage(chatId, "Вы еще не подписались на канал. Пожалуйста, подпишитесь и нажмите \"Проверить подписку\". 📢");
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

                sendMessage(chatId, "Напоминание установлено на " + formattedReminderTime + ". ⏰");
            }
        } else if (data.startsWith("deleteOk_")) {
            int reminderId = Integer.parseInt(data.split("_")[1]);
            deleteReminder(chatId, reminderId);
        } else if ("confirm_no".equals(data)) {
            reminderCreationStates.remove(chatId);
            sendMessage(chatId, "Создание напоминания отменено. ❌");
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
        } else if ("ADD_TO_BUDGET_YES".equals(data)) {
            addIncomeToBudget(chatId);
        } else if ("ADD_TO_BUDGET_NO".equals(data)) {
            finalizeIncomeCreation(chatId);
        } else if (data.equals("confirm_update_income_add_to_budget")) {
            IncomeUpdateState currentState = incomeUpdateStates.get(chatId);
            if (currentState != null) {
                Income updatedIncome = incomeService.findById(currentState.getIncomeId());
                if (updatedIncome != null) {
                    double oldAmount = currentState.getOriginalAmount();
                    double newAmount = updatedIncome.getAmount();
                    double amountDifference = newAmount - oldAmount;

                    boolean budgetUpdated = adjustBudgetForIncome(chatId, amountDifference);

                    if (budgetUpdated) {
                        sendMessage(chatId, "Изменения сохранены и бюджет обновлен. 💼");
                    } else {
                        sendMessage(chatId, "Изменения сохранены. Для внесения изменений в бюджет необходимо создать его. 📉");
                    }

                    incomeUpdateStates.remove(chatId);
                } else {
                    sendMessage(chatId, "Ошибка при обновлении дохода. \uD83D\uDEAB");
                }
            }
        } else if (data.startsWith("update_budget_")) {
            handleUpdateBudget(data, chatId);
        } else if (data.startsWith("delete_budget_")) {
            handleDeleteBudget(data, chatId);
        } else if ("DEDUCT_FROM_BUDGET_YES".equals(data)) {
            deductExpenseFromBudget(chatId, expenseState);
        } else if ("DEDUCT_FROM_BUDGET_NO".equals(data)) {
            finalizeExpenseCreation(chatId);
        } else if ("confirm_update_expense_change_budget".equals(data)) {
            ExpenseUpdateState currentState = expenseUpdateStates.get(chatId);
            if (currentState != null) {
                Expense updatedExpense = expenseService.findById(currentState.getExpenseId());
                if (updatedExpense != null) {
                    double oldAmount = currentState.getOriginalAmount();
                    double newAmount = updatedExpense.getAmount();
                    double amountDifference = newAmount - oldAmount;

                    boolean budgetUpdated = adjustBudgetForExpense(chatId, amountDifference);

                    if (budgetUpdated) {
                        sendMessage(chatId, "Изменения и корректировка бюджета успешно сохранены. 💼");
                    } else {
                        sendMessage(chatId, "Изменения сохранены. Для корректировки бюджета необходимо создать его. 📉");
                    }

                    expenseUpdateStates.remove(chatId);
                } else {
                    sendMessage(chatId, "Ошибка при обновлении расхода. \uD83D\uDEAB");
                }
            }
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
                sendMessage(chatId, "❗ Неверная команда. Пожалуйста, попробуйте снова.");
                break;
        }
    }

    private void handleUpdateIncome(String data, String chatId) {
        String incomeIdString = data.substring("update_income_".length());
        try {
            Long incomeId = Long.parseLong(incomeIdString);
            Income income = incomeService.findById(incomeId);
            if (income == null || !income.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "🚫 Доход с указанным номером не найден или не принадлежит вам.");
                return;
            }
            incomeUpdateStates.put(chatId, new IncomeUpdateState(incomeId, "", income));
            sendFieldSelectionMessageForIncome(chatId);
            sendMessage(chatId, "📊 Пожалуйста, выберите поле для обновления дохода.");
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при выборе дохода для обновления. Попробуйте снова.");
        }
    }

    public void handleUpdateExpense(String data, String chatId) {
        String expenseIdString = data.substring("update_expense_".length());
        try {
            Long expenseId = Long.parseLong(expenseIdString);
            Expense expense = expenseService.findById(expenseId);
            if (expense == null || !expense.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "💸 Расход с указанным номером не найден или не принадлежит вам.");
                return;
            }
            expenseUpdateStates.put(chatId, new ExpenseUpdateState(expenseId, expense,""));
            sendFieldSelectionMessageForExpense(chatId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при выборе расхода для обновления. Попробуйте снова.");
        }
    }

    public void handleUpdateBudget(String data, String chatId) {
        String budgetIdString = data.substring("update_budget_".length());

        try {
            Long budgetId = Long.parseLong(budgetIdString);

            Budget budget = budgetService.findById(budgetId);

            if (budget == null || !budget.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "💼 Бюджет с указанным номером не найден или не принадлежит вам.");
                return;
            }

            budgetUpdateStates.put(chatId, new BudgetUpdateState(budgetId, "", budget));
            sendFieldSelectionMessageForBudget(chatId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при выборе бюджета для обновления. Попробуйте снова.");
        }
    }

    private void handleUpdateReminder(String data, String chatId) {
        String reminderIdString = data.substring("update_reminder_".length());
        try {
            int reminderId = Integer.parseInt(reminderIdString);
            Optional<Reminder> optionalReminder = reminderService.findReminderById(reminderId);

            if (optionalReminder.isEmpty()) {
                sendMessage(chatId, "🕒 Напоминание с указанным номером не найдено.");
                return;
            }

            Reminder reminder = optionalReminder.get();

            if (!reminder.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "🚫 Напоминание не принадлежит вам.");
                return;
            }

            reminderUpdateStates.put(chatId, new ReminderUpdateState((long) reminderId, "", reminder));
            sendFieldSelectionMessageForReminder(chatId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при выборе напоминания для обновления. Попробуйте снова.");
        }
    }

    private void handleSetDeadlineTask(String data, String chatId) {
        String taskIdString = data.substring("set_deadline_".length());
        try {
            int taskId = Integer.parseInt(taskIdString);
            Task task = taskService.findById(taskId);
            if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "❌ Задача с указанным номером не найдена или не принадлежит вам.");
                return;
            }
            taskDeadlineStates.put(chatId, taskId);
            sendMessage(chatId, "📅 Введите дедлайн для задачи в формате ГГГГ-ММ-ДД ЧЧ:ММ.");
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при выборе задачи. Попробуйте снова.");
        }
    }

    private void handleUpdateTask(String data, String chatId) {
        String taskIdString = data.substring("update_task_".length());
        try {
            int taskId = Integer.parseInt(taskIdString);
            Task task = taskService.findById(taskId);
            if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "❌ Задача с указанным номером не найдена или не принадлежит вам.");
                return;
            }
            taskUpdateStates.put(chatId, new TaskUpdateState(taskId, "", task));
            sendFieldSelectionMessage(chatId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при выборе задачи для обновления. Попробуйте снова.");
        }
    }

    private void handleDeleteTask(String data, String chatId) {
        String taskIdString = data.substring("delete_task_".length());
        try {
            int taskId = Integer.parseInt(taskIdString);
            Task task = taskService.findById(taskId);
            if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "❌ Задача с указанным номером не найдена или не принадлежит вам.");
                return;
            }
            taskDeletionStates.put(chatId, Collections.singletonList(taskId));
            sendDeleteConfirmationMessage(chatId, taskId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при удалении задачи. Попробуйте снова.");
        }
    }

    private void handleChangeStatus(String data, String chatId) {
        String taskIdString = data.substring("change_status_".length());
        try {
            int taskId = Integer.parseInt(taskIdString);
            Task task = taskService.findById(taskId);
            if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "❌ Задача с указанным номером не найдена или не принадлежит вам.");
                return;
            }
            sendStatusChangeMessage(chatId, taskId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при выборе задачи для изменения статуса. Попробуйте снова.");
        }
    }

    private void handleStatusChange(String data, String chatId) {
        if (data.startsWith("status_completed_")) {
            boolean isCompleted = true;
            changeTaskStatus(chatId, data, isCompleted);
        } else if (data.startsWith("status_cancel_change_")) {
            sendMessage(chatId, "❌ Изменение статуса задачи отменено.");
        }
    }

    private void handleDeleteReminder(String data, String chatId) {
        String reminderIdString = data.substring("delete_reminder_".length());
        try {
            Long reminderId = Long.parseLong(reminderIdString);
            Optional<Reminder> optionalReminder = reminderService.findReminderById(Math.toIntExact(reminderId));
            if (optionalReminder.isEmpty() || !optionalReminder.get().getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "❌ Напоминание с указанным номером не найдено или не принадлежит вам.");
                return;
            }

            reminderDeletionStates.put(chatId, Collections.singletonList(reminderId));
            sendDeleteReminderConfirmationMessage(chatId, reminderId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при удалении напоминания. Попробуйте снова.");
        }
    }

    private void handleDeleteIncome(String data, String chatId) {
        String incomeIdString = data.substring("delete_income_".length());
        try {
            int incomeId = Integer.parseInt(incomeIdString);
            Income income = incomeService.findById((long) incomeId);
            if (income == null || !income.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "❌ Запись о доходе с указанным номером не найдена или не принадлежит вам.");
                return;
            }
            incomeDeletionStates.put(chatId, Collections.singletonList(incomeId));
            sendDeleteIncomeConfirmationMessage(chatId, incomeId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при удалении записи о доходе. Попробуйте снова.");
        }
    }

    private void handleDeleteExpense(String data, String chatId) {
        String expenseIdString = data.substring("delete_expense_".length());
        try {
            long expenseId = Long.parseLong(expenseIdString);
            Expense expense = expenseService.findById(expenseId);
            if (expense == null || !expense.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "❌ Запись о расходе с указанным номером не найдена или не принадлежит вам.");
                return;
            }
            expenseDeletionStates.put(chatId, Collections.singletonList(Math.toIntExact(expenseId)));
            sendDeleteExpenseConfirmationMessage(chatId, Math.toIntExact(expenseId));
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при удалении записи о расходе. Попробуйте снова.");
        }
    }

    private void handleDeleteBudget(String data, String chatId) {
        String budgetIdString = data.substring("delete_budget_".length());
        try {
            long budgetId = Long.parseLong(budgetIdString);
            Budget budget = budgetService.findById(budgetId);
            if (budget == null || !budget.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "❌ Бюджет с указанным номером не найден или не принадлежит вам.");
                return;
            }
            budgetDeletionStates.put(chatId, budgetId);
            sendDeleteBudgetConfirmationMessage(chatId, budgetId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при удалении записи о бюджете. Попробуйте снова.");
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
        BudgetUpdateState budgetState = budgetUpdateStates.get(chatId);
        Long budgetId = budgetDeletionStates.get(chatId);

        if (currentState == null && curState == null && currentIncomeState == null &&
                (taskIds == null || taskIds.isEmpty()) && (remindersId == null || remindersId.isEmpty()) &&
                incomeState == null && expenseUpdateState == null && (incomeIds == null || incomeIds.isEmpty()) &&
                (expenseIds == null || expenseIds.isEmpty()) && budgetState == null && budgetId == null) {
            sendMessage(chatId, "❌ Ошибка при обработке запроса.");
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
                sendMessage(chatId, "✅ Изменения подтверждены.");
                break;

            case "cancel_update", "update_cancel":
                assert currentState != null;
                cancelUpdate(chatId, currentState);
                break;

            case "confirm_update_reminder":
                reminderUpdateStates.remove(chatId);
                sendMessage(chatId, "✅ Изменения подтверждены.");
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
                    sendMessage(chatId, "❌ Ошибка при подтверждении удаления задачи.");
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
                    sendMessage(chatId, "❌ Ошибка при подтверждении удаления напоминания.");
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
                sendMessage(chatId, "❌ Обновление дохода отменено.");
                break;

            case "cancel_update_income":
                assert incomeState != null;
                cancelIncomeUpdate(chatId, incomeState);
                break;

            case "confirm_update_income", "confirm_update_income_add_to_budget":
                incomeUpdateStates.remove(chatId);
                sendMessage(chatId, "✅ Изменения подтверждены.");
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

            case "update_date_expense":
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
                sendMessage(chatId, "❌ Обновление расхода отменено.");
                break;

            case "confirm_update_expense":
                expenseUpdateStates.remove(chatId);
                sendMessage(chatId, "✅ Изменения подтверждены.");
                break;

            case "cancel_update_expense":
                assert expenseUpdateState != null;
                cancelExpenseUpdate(chatId, expenseUpdateState);
                break;

            case "confirm_delete_income":
                if (incomeIds != null && !incomeIds.isEmpty()) {
                    confirmDeleteIncome(chatId, incomeIds);
                    incomeDeletionStates.remove(chatId);
                } else {
                    sendMessage(chatId, "❌ Ошибка при подтверждении удаления записи о доходе.");
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
                    sendMessage(chatId, "❌ Ошибка при подтверждении удаления записи о доходе.");
                }
                break;

            case "cancel_delete_expense":
                cancelDeleteExpense(chatId);
                break;

            case "update_name_budget":
                assert budgetState != null;
                budgetState.setFieldToUpdate("name");
                sendNewValueRequestForBudget(chatId, "name");
                break;

            case "update_amount_budget":
                assert budgetState != null;
                budgetState.setFieldToUpdate("amount");
                sendNewValueRequestForBudget(chatId, "amount");
                break;

            case "update_start_date_budget":
                assert budgetState != null;
                budgetState.setFieldToUpdate("start_date");
                sendNewValueRequestForBudget(chatId, "start_date");
                break;

            case "update_end_date_budget":
                assert budgetState != null;
                budgetState.setFieldToUpdate("end_date");
                sendNewValueRequestForBudget(chatId, "end_date");
                break;

            case "update_category_budget":
                assert budgetState != null;
                budgetState.setFieldToUpdate("category");
                sendNewValueRequestForBudget(chatId, "category");
                break;

            case "update_warning_threshold_budget":
                assert budgetState != null;
                budgetState.setFieldToUpdate("warning_threshold");
                sendNewValueRequestForBudget(chatId, "warning_threshold");
                break;

            case "update_cancel_budget":
                budgetUpdateStates.remove(chatId);
                sendMessage(chatId, "❌ Обновление бюджета отменено.");
                break;

            case "confirm_update_budget":
                budgetUpdateStates.remove(chatId);
                sendMessage(chatId, "✅ Изменения подтверждены.");
                break;

            case "cancel_update_budget":
                assert budgetState != null;
                cancelBudgetUpdate(chatId, budgetState);
                break;

            case "confirm_delete_budget":
                confirmDeleteBudget(chatId, budgetId);
                budgetDeletionStates.remove(chatId);
                break;

            case "cancel_delete_budget":
                cancelDeleteBudget(chatId);
                break;

            default:
                sendMessage(chatId, "❓ Неверный выбор.");
                break;
        }
    }

    private void handleChangeStatusCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals(BUTTON_CHANGE_STATUS_TASK_TEXT))) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте команду /change_status только без параметров.");
            return;
        }

        List<Task> tasks = taskService.findTasksByUserId(Long.parseLong(chatId));

        if (tasks.isEmpty()) {
            sendMessage(chatId, "У вас пока нет задач для изменения статуса. 😔");
            return;
        }

        InlineKeyboardMarkup markup = createTasksStatusMarkup(tasks);

        SendMessage message = createMessage(chatId, "Выберите задачу для изменения статуса: 📝", markup);

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
                sendMessage(chatId, "❌ Задача с указанным номером не найдена или не принадлежит вам.");
                return;
            }

            task.setCompleted(isCompleted);
            taskService.save(task);

            sendMessage(chatId, "✅ Статус задачи '" + task.getTitle() + "' изменен на " + (isCompleted ? "Завершена" : "Не завершена") + ".");
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка при изменении статуса задачи.");
        }
    }

    private void sendStatusChangeMessage(String chatId, int taskId) {
        Task task = taskService.findById(taskId);
        if (task == null) {
            sendMessage(chatId, "Задача не найдена.");
            return;
        }

        StringBuilder statusMessage = new StringBuilder();
        statusMessage.append("Выберите новый статус для задачи:\n\n");
        statusMessage.append("📝 Название: ").append(task.getTitle()).append("\n");
        statusMessage.append("\uD83D\uDCCA Текущий статус: ").append(task.isCompleted() ? "✅ Завершена" : "⭕ Не завершена").append("\n\n");
        statusMessage.append("🔔 Примечание: после смены статуса задачи на 'Завершена', задача будет удалена.\n");

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
        completedButton.setText(BUTTON_COMPLETE);
        completedButton.setCallbackData("status_completed_" + taskId);

        InlineKeyboardButton cancelChangeButton = new InlineKeyboardButton();
        cancelChangeButton.setText(BUTTON_CANCEL_CHANGE);
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
        if (parts.length > 1 && !messageText.equals(BUTTON_LIST_TASKS_TEXT)) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте команду /list_tasks только без параметров.");
            return;
        }

        List<Task> tasks = taskService.getTasksByUserChatId(Long.parseLong(chatId));
        if (tasks.isEmpty()) {
            sendMessage(chatId, EmojiParser.parseToUnicode(":information_source: У вас нет задач."));
            return;
        }

        tasks.sort(Comparator.comparingInt(Task::getPriority).reversed());

        StringBuilder messageBuilder = new StringBuilder();
        for (Task task : tasks) {
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDD8A Название: ")).append(task.getTitle()).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCC4 Описание: ")).append(task.getDescription() != null ? task.getDescription() : "Без описания").append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCC5 Создано: ")).append(formatTimestampTime(task.getCreationTimestamp(), chatId)).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode("⭐ Приоритет: ")).append(task.getPriority()).append("\n");
            if (task.getDeadline() != null) {
                messageBuilder.append(EmojiParser.parseToUnicode("⏰ Дедлайн: ")).append(formatDeadline(task.getDeadline(), chatId)).append("\n");
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

    private String formatTimestampTime(Timestamp timestamp, String chatId) {
        LocalDateTime localDateTime = timestamp.toLocalDateTime();
        ZoneId zoneId = ZoneId.of(userService.getUserTimezone(chatId));
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zoneId);
        return zonedDateTime.format(formatter);
    }

    private String formatDeadline(LocalDateTime deadline, String chatId) {
        ZoneId zoneId = ZoneId.of(userService.getUserTimezone(chatId));
        ZonedDateTime zonedDateTime = deadline.atZone(ZoneId.of("UTC")).withZoneSameInstant(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zoneId);
        return zonedDateTime.format(formatter);
    }

    private void handleSetDeadlineCommand(String chatId, String[] parts, String messageText) {
        if (parts.length > 1 && !(messageText.equals(BUTTON_SET_DEADLINE_TEXT))) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте команду /set_deadline только без параметров.");
            return;
        }

        List<Task> tasks = taskService.findTasksByUserId(Long.parseLong(chatId));

        if (tasks.isEmpty()) {
            sendMessage(chatId, "У вас пока нет задач для установки дедлайна. " + EmojiParser.parseToUnicode(":pensive:"));
            return;
        }

        InlineKeyboardMarkup markup = createDeadlineTaskMarkup(tasks);
        SendMessage message = createMessage(chatId, EmojiParser.parseToUnicode(":alarm_clock: Выберите задачу для установки дедлайна:"), markup);

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

        User user = userService.findByChatId(Long.parseLong(chatId));
        String userTimezone = user.getTimezone();
        if (userTimezone == null || userTimezone.isEmpty()) {
            sendMessage(chatId, "Не удалось определить ваш часовой пояс. Пожалуйста, сначала установите его с помощью команды /update_timezone.");
            taskDeadlineStates.remove(chatId);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime deadline;
        try {
            deadline = LocalDateTime.parse(deadlineInput, formatter);
        } catch (DateTimeParseException e) {
            sendMessage(chatId, "Неверный формат даты. Пожалуйста, введите дедлайн в формате ГГГГ-ММ-ДД ЧЧ:ММ. 📅");
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of(userTimezone));
        if (deadline.isBefore(now) || deadline.isEqual(now)) {
            sendMessage(chatId, "Дата дедлайна должна быть в будущем. Пожалуйста, введите корректную дату. ⏰");
            return;
        }

        ZonedDateTime userDeadline = deadline.atZone(ZoneId.of(userTimezone));
        ZonedDateTime utcDeadline = userDeadline.withZoneSameInstant(ZoneId.of("UTC"));

        task.setDeadline(utcDeadline.toLocalDateTime());
        taskService.save(task);
        taskDeadlineStates.remove(chatId);
        sendMessage(chatId, "Дедлайн установлен для задачи: " + task.getTitle() + " 🚀");
    }

    private void handleReminderCreationCommand(String chatId, String[] parts, String messageText) {
        if (parts.length > 1 && !(messageText.equals(BUTTON_CREATE_REMINDER_TEXT))) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте команду /create_reminder только без параметров.");
            return;
        }

        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "🚫 Пользователь не зарегистрирован. Используйте /start для регистрации.");
            return;
        }

        int existingRemindersCount = reminderService.countByUser(user);
        if (existingRemindersCount > 20) {
            sendMessage(chatId, "⚠️ Вы уже создали максимальное количество напоминаний (20 штук).");
            return;
        }

        reminderCreationStates.put(chatId, new ReminderCreationState());
        sendMessage(chatId, "✨ Введите текст напоминания:");
    }

    private void processReminderCreation(String chatId, String messageText) {
        ReminderCreationState currentState = reminderCreationStates.get(chatId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        if (currentState.getState() == ReminderState.ENTER_MESSAGE) {
            currentState.setMessage(messageText);
            currentState.setState(ReminderState.ENTER_REMINDER_TIME);
            sendMessage(chatId, "\uD83D\uDCDD Введите время напоминания в формате yyyy-MM-dd HH:mm");
        } else if (currentState.getState() == ReminderState.ENTER_REMINDER_TIME) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(messageText, formatter);

                LocalDateTime currentDateTime = LocalDateTime.now();
                if (localDateTime.isBefore(currentDateTime)) {
                    sendMessage(chatId, "⏰ Время напоминания не может быть в прошлом или текущее. Пожалуйста, введите корректное время:");
                    return;
                }

                Timestamp reminderTime = Timestamp.valueOf(localDateTime);
                currentState.setReminderTime(reminderTime);
                currentState.setState(ReminderState.CONFIRMATION);

                String confirmationMessage = "📅 Вы ввели следующие данные:\n" +
                        "Сообщение: " + currentState.getMessage() + "\n" +
                        "Время напоминания: " + localDateTime.format(formatter) + "\n\n" +
                        "✅ Все верно?";
                sendConfirmationMessage(chatId, confirmationMessage);
            } catch (DateTimeParseException e) {
                sendMessage(chatId, "❌ Неверный формат времени. Пожалуйста, введите время в формате yyyy-MM-dd HH:mm:");
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
        row1.add(createInlineButtonForRemind(BUTTON_CONFIRM, "confirm_yes"));
        row1.add(createInlineButtonForRemind(BUTTON_CANCEL_UPDATE, "confirm_no"));
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
            sendMessage(chatId, "🚫 Пользователь не зарегистрирован. Используйте /start для регистрации.");
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
        String messageText = "🔔 Напоминание: " + reminder.getMessage();

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
                createInlineButtonForRemind(BUTTON_CHANGE_5MIN, "reschedule_5m_" + reminderId),
                createInlineButtonForRemind(BUTTON_CHANGE_1HOUR, "reschedule_1h_" + reminderId)
        ));
        keyboard.add(Arrays.asList(
                createInlineButtonForRemind(BUTTON_CHANGE_1DAY, "reschedule_1d_" + reminderId),
                createInlineButtonForRemind(BUTTON_CHANGE_ON_YOUR_TIME, "reschedule_custom_" + reminderId)
        ));
        keyboard.add(Collections.singletonList(
                createInlineButtonForRemind(BUTTON_OK_AND_DELETE, "deleteOk_" + reminderId)
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
                sendMessage(chatId, "🕒 Напоминание успешно отложено на " + duration.toMinutes() + " минут.");
            } else {
                sendMessage(chatId, "❌ Ошибка при отложении напоминания.");
            }
        } else {
            sendMessage(chatId, "❓ Напоминание не найдено.");
        }
    }

    public void askForCustomTime(String chatId, int reminderId) {
        reminderCustomTimeStates.put(chatId, reminderId);
        sendMessage(chatId, "🕒 Введите новое время напоминания в формате yyyy-MM-dd HH:mm:");
    }

    public void processCustomTimeInput(String chatId, String messageText) {
        Integer reminderId = reminderCustomTimeStates.get(chatId);
        if (reminderId != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime localDateTime = LocalDateTime.parse(messageText, formatter);

                LocalDateTime currentDateTime = LocalDateTime.now();
                if (localDateTime.isBefore(currentDateTime)) {
                    sendMessage(chatId, "⏰ Время напоминания не может быть в прошлом или текущее. Пожалуйста, введите корректное время.");
                    return;
                }

                Optional<Reminder> optionalReminder = reminderService.findReminderById(reminderId);
                if (optionalReminder.isPresent()) {
                    Reminder reminder = optionalReminder.get();
                    Timestamp newTime = Timestamp.valueOf(localDateTime);
                    reminder.setReminderTime(newTime);
                    reminder.setSent(false);
                    reminderService.save(reminder);
                    sendMessage(chatId, "🕒 Напоминание успешно отложено на " + localDateTime.format(formatter) + ".");
                    reminderCustomTimeStates.remove(chatId);
                } else {
                    sendMessage(chatId, "❌ Ошибка при отложении напоминания.");
                }
            } catch (DateTimeParseException e) {
                sendMessage(chatId, "❌ Неверный формат времени. Пожалуйста, введите время в формате yyyy-MM-dd HH:mm:");
            }
        } else {
            sendMessage(chatId, "❌ Ошибка при отложении напоминания.");
        }
    }

    private void deleteReminder(String chatId, int reminderId) {
        reminderService.deleteById(reminderId);
        sendMessage(chatId, "✅ Напоминание выполнено и успешно удалено.");
    }

    private void handleUpdateReminderCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals(BUTTON_UPDATE_REMINDER_TEXT))) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте команду /update_reminder только без параметров.");
            return;
        }

        List<Reminder> reminders = reminderService.findRemindersByUserId(Long.parseLong(chatId));

        if (reminders.isEmpty()) {
            sendMessage(chatId, "😔 У вас пока нет напоминаний для обновления.");
            return;
        }

        InlineKeyboardMarkup markup = createRemindersMarkup(reminders);

        SendMessage message = createMessage(chatId, "📝 Выберите напоминание для обновления:", markup);

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
            button.setText("📌 Напоминание #" + reminder.getId());
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
            sendMessage(chatId, "❌ Ошибка обновления напоминания.");
            return;
        }

        int reminderId = Math.toIntExact(currentState.getReminderId());

        Optional<Reminder> optionalReminder = reminderService.findReminderById(reminderId);
        if (optionalReminder.isEmpty()) {
            sendMessage(chatId, "❓ Напоминание с указанным номером не найдено или не принадлежит вам.");
            return;
        }

        Reminder reminder = optionalReminder.get();

        if (!reminder.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "❌ Напоминание не принадлежит вам.");
            return;
        }

        String currentMessage = reminder.getMessage();
        LocalDateTime remindAt = reminder.getReminderTime().toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        String selectionMessage = "📝️ Выберите, что вы хотите обновить для напоминания:\n";
        selectionMessage += "📌 Текущее сообщение: " + currentMessage + "\n";
        selectionMessage += "🕒 Дата напоминания: " + remindAt.format(formatter) + "\n";

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
        row2.add(createInlineButton(BUTTON_CANCEL, "update_cancel_reminder"));
        keyboard.add(row2);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void processFieldAndValueForReminder(String chatId, String messageText) {
        ReminderUpdateState currentState = reminderUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "❌ Ошибка при обновлении напоминания.");
            return;
        }

        int reminderId = Math.toIntExact(currentState.getReminderId());

        Optional<Reminder> optionalReminder = reminderService.findReminderById(reminderId);
        if (optionalReminder.isEmpty()) {
            sendMessage(chatId, "❓ Напоминание с указанным номером не найдено или не принадлежит вам.");
            reminderUpdateStates.remove(chatId);
            return;
        }

        Reminder reminder = optionalReminder.get();
        if (!reminder.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "❌ Напоминание не принадлежит вам.");
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
                    sendMessage(chatId, "❌ Неверный формат даты и времени. Используйте формат yyyy-MM-dd HH:mm");
                    return;
                }
                break;
            default:
                sendMessage(chatId, "❌ Неизвестное поле для обновления: " + fieldToUpdate);
                return;
        }

        reminderService.save(reminder);

        sendConfirmationMessageForReminder(chatId, reminder);
    }

    private void sendConfirmationMessageForReminder(String chatId, Reminder reminder) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("✨ Изменения сохранены:\n");
        confirmationMessage.append("📝 Сообщение: ").append(reminder.getMessage()).append("\n");

        LocalDateTime remindAt = reminder.getReminderTime().toLocalDateTime();
        if (remindAt != null) {
            confirmationMessage.append("⏰ Напомнить в: ").append(remindAt.format(formatter)).append("\n");
        }

        confirmationMessage.append("\n\n❓ Подтвердить изменения?");

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

        List<InlineKeyboardButton> row = createButtonRow(BUTTON_CONFIRM, "confirm_update_reminder");
        row.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_update_reminder"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void cancelReminderUpdate(String chatId, ReminderUpdateState currentState) {
        reminderService.save(currentState.getOriginalReminder());
        reminderUpdateStates.remove(chatId);
        sendMessage(chatId, "❌ Изменения в напоминании отменены.");
    }

    private void sendNewValueRequestForReminder(String chatId, String field) {
        String messageText;
        switch (field) {
            case "message":
                messageText = "📝 Введите новое сообщение для напоминания:";
                break;
            case "remindAt":
                messageText = "⏰ Введите новую дату и время напоминания в формате yyyy-MM-dd HH:mm";
                break;
            default:
                log.error("Unsupported field type for reminder: {}", field);
                return;
        }

        sendMessage(chatId, messageText);
    }

    private void handleDeleteReminderCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals(BUTTON_DELETE_REMINDER_TEXT))) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /delete_reminder без параметров.");
            return;
        }

        List<Reminder> reminders = reminderService.findRemindersByUserId(Long.parseLong(chatId));

        if (reminders.isEmpty()) {
            sendMessage(chatId, "ℹ️ У вас нет напоминаний для удаления.");
            return;
        }

        InlineKeyboardMarkup markup = createDeleteReminderMarkup(reminders);

        SendMessage message = createMessage(chatId, "🗑️ Выберите напоминание для удаления:", markup);

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
            button.setText("🗑️ Напоминание #" + reminder.getId());
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
        if (optionalReminder.isEmpty()) {
            sendMessage(chatId, "❌ Напоминание не найдено.");
            return;
        }

        Reminder reminder = optionalReminder.get();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("❗ Вы уверены, что хотите удалить следующее напоминание?\n\n");
        confirmationMessage.append("📌 Сообщение: ").append(reminder.getMessage()).append("\n");
        confirmationMessage.append("⏰ Время напоминания: ").append(reminder.getReminderTime().toLocalDateTime().format(formatter)).append("\n");

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
        sendMessage(chatId, "✅️ Напоминание удалено.");
    }

    private void cancelDeleteReminder(String chatId) {
        reminderDeletionStates.remove(chatId);
        sendMessage(chatId, "❌ Удаление напоминания отменено.");
    }

    private void handleListRemindersCommand(String chatId, String[] parts, String messageText) {
        if (parts.length > 1 && !(messageText.equals(BUTTON_LIST_REMINDERS_TEXT))) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /list_reminders без параметров.");
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
        if (parts.length > 1 && !messageText.equals(BUTTON_ADD_INCOME_TEXT)) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /add_income без параметров.");
            return;
        }

        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "👤 Пользователь не зарегистрирован. Используйте /start для регистрации.");
            return;
        }

        int existingIncomeCount = incomeService.countByUser(user);
        if (existingIncomeCount > 25) {
            sendMessage(chatId, "📊 Вы уже создали максимальное количество записей о доходах (25 штук).");
            return;
        }

        startIncomeCreation(chatId);
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
        message.setText("💵 Выберите тип дохода или введите своё значение:");
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
                sendMessage(chatId, "💰 Введите сумму дохода для '" + messageText + "' (в рублях):");

            } else if ("Другое".equals(messageText)) {
                currentState.setTitle("Другое");
                currentState.setState(IncomeState.ENTER_CUSTOM_TITLE);
                sendMessage(chatId, "📝 Введите тип дохода:");

            } else {
                sendMessage(chatId, "⚠️ Пожалуйста, выберите тип дохода из предложенных кнопок.");
            }

        } else if (currentState.getState() == IncomeState.ENTER_CUSTOM_TITLE) {
            currentState.setTitle(messageText);
            currentState.setState(IncomeState.ENTER_AMOUNT);
            sendMessage(chatId, "💰 Введите сумму дохода для '" + messageText + "' (в рублях):");

        } else if (currentState.getState() == IncomeState.ENTER_AMOUNT) {
            try {
                double amount = Double.parseDouble(messageText);
                currentState.setAmount(amount);
                currentState.setState(IncomeState.ENTER_DATE);
                sendMessage(chatId, "📅 Введите дату дохода (в формате ГГГГ-ММ-ДД):");
            } catch (NumberFormatException e) {
                sendMessage(chatId, "❌ Пожалуйста, введите корректное числовое значение для суммы дохода:");
            }
        } else if (currentState.getState() == IncomeState.ENTER_DATE) {
            try {
                Timestamp date = Timestamp.valueOf(messageText + " 00:00:00");
                currentState.setDate(date);
                currentState.setState(IncomeState.ENTER_DESCRIPTION);
                sendMessage(chatId, "📝 Введите описание дохода:");
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, "❌ Пожалуйста, введите корректную дату в формате ГГГГ-ММ-ДД:");
            }
        } else if (currentState.getState() == IncomeState.ENTER_DESCRIPTION) {
            currentState.setDescription(messageText);
            currentState.setState(IncomeState.ENTER_CATEGORY);
            sendMessage(chatId, "📂 Введите категорию дохода:");
        } else if (currentState.getState() == IncomeState.ENTER_CATEGORY) {
            currentState.setCategory(messageText);

            sendAddToBudgetPrompt(chatId, currentState);
        }
    }

    private void sendAddToBudgetPrompt(String chatId, IncomeCreationState currentState) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        String formattedAmount;
        if (currentState.getAmount() == Math.floor(currentState.getAmount())) {
            formattedAmount = String.format("%.0f", currentState.getAmount());
        } else {
            formattedAmount = String.format("%.2f", currentState.getAmount());
        }

        message.setText("💸 Добавить сумму " + formattedAmount + " руб. к текущему бюджету?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(BUTTON_CONFIRM);
        yesButton.setCallbackData("ADD_TO_BUDGET_YES");

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(BUTTON_CANCEL_UPDATE);
        noButton.setCallbackData("ADD_TO_BUDGET_NO");

        buttons.add(Arrays.asList(yesButton, noButton));
        markup.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        sendMessageForIncome(message);
    }

    private void addIncomeToBudget(String chatId) {
        IncomeCreationState currentState = incomeCreationStates.get(chatId);

        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "❌ Пользователь не зарегистрирован. Используйте /start для регистрации. 📝");
            return;
        }

        Budget existingBudget = budgetService.findByUserChatId(user.getChatId());
        if (existingBudget != null) {
            existingBudget.setAmount(existingBudget.getAmount() + currentState.getAmount());
            budgetService.save(existingBudget);
        } else {
            sendMessage(chatId, "⚠️ Бюджет еще не создан. Создайте бюджет, чтобы добавить доход. 📉");
        }

        finalizeIncomeCreation(chatId);
    }

    private void finalizeIncomeCreation(String chatId) {
        IncomeCreationState currentState = incomeCreationStates.get(chatId);

        createIncome(currentState.getTitle(), currentState.getAmount(), currentState.getDate(),
                currentState.getDescription(), currentState.getCategory(), chatId);

        incomeCreationStates.remove(chatId);

        String formattedAmount;
        if (currentState.getAmount() == Math.floor(currentState.getAmount())) {
            formattedAmount = String.format("%.0f", currentState.getAmount());
        } else {
            formattedAmount = String.format("%.2f", currentState.getAmount());
        }

        sendMessage(chatId, "🎉 Доход '" + currentState.getTitle() + "' с суммой " + formattedAmount + " руб. успешно создан! 💸");
    }

    private void createIncome(String title, double amount, Timestamp date, String description, String category, String chatId) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "🚫 Пользователь не зарегистрирован. Используйте /start для регистрации.");
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
        button1_1.setText(BUTTON_SALARY);
        button1_1.setCallbackData("Заработная плата");
        row1.add(button1_1);

        InlineKeyboardButton button1_2 = new InlineKeyboardButton();
        button1_2.setText(BUTTON_PENSION);
        button1_2.setCallbackData("Пенсия");
        row1.add(button1_2);

        rowsInline.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2_1 = new InlineKeyboardButton();
        button2_1.setText(BUTTON_SCHOLARSHIP);
        button2_1.setCallbackData("Стипендия");
        row2.add(button2_1);

        InlineKeyboardButton button2_2 = new InlineKeyboardButton();
        button2_2.setText(BUTTON_STIPEND);
        button2_2.setCallbackData("Пособие");
        row2.add(button2_2);

        rowsInline.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3_1 = new InlineKeyboardButton();
        button3_1.setText(BUTTON_INCOME_FROM_THE_SALE_OF_GOODS);
        button3_1.setCallbackData("Доход от продажи товаров");
        row3.add(button3_1);

        InlineKeyboardButton button3_2 = new InlineKeyboardButton();
        button3_2.setText(BUTTON_OTHER);
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
        if (parts.length > 1 && !messageText.equals(BUTTON_ADD_EXPENSE_TEXT)) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /add_expense без параметров.");
            return;
        }

        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "🚫 Пользователь не зарегистрирован. Используйте /start для регистрации.");
            return;
        }

        int existingExpenseCount = expenseService.countByUser(user);
        if (existingExpenseCount > 25) {
            sendMessage(chatId, "⚠️ Вы уже создали максимальное количество записей о расходах (25 штук).");
            return;
        }

        startExpenseCreation(chatId);
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
        message.setText("💸 Выберите тип расхода или введите своё значение:");
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
                sendMessage(chatId, "💰 Введите сумму расхода для '" + messageText + "' (в рублях):");

            } else if ("Другое".equals(messageText)) {
                currentState.setTitle("Другое");
                currentState.setState(ExpenseState.ENTER_CUSTOM_TITLE);
                sendMessage(chatId, "✏️ Введите тип расхода:");

            } else {
                sendMessage(chatId, "🚫 Пожалуйста, выберите тип расхода из предложенных кнопок.");
            }

        } else if (currentState.getState() == ExpenseState.ENTER_CUSTOM_TITLE) {
            currentState.setTitle(messageText);
            currentState.setState(ExpenseState.ENTER_AMOUNT);
            sendMessage(chatId, "💵 Введите сумму расхода для '" + messageText + "' (в рублях):");

        } else if (currentState.getState() == ExpenseState.ENTER_AMOUNT) {
            try {
                double amount = Double.parseDouble(messageText);
                currentState.setAmount(amount);
                currentState.setState(ExpenseState.ENTER_DATE);
                sendMessage(chatId, "📅 Введите дату расхода (в формате ГГГГ-ММ-ДД):");
            } catch (NumberFormatException e) {
                sendMessage(chatId, "⚠️ Пожалуйста, введите корректное числовое значение для суммы расхода:");
            }
        } else if (currentState.getState() == ExpenseState.ENTER_DATE) {
            try {
                Timestamp date = Timestamp.valueOf(messageText + " 00:00:00");
                currentState.setDate(date);
                currentState.setState(ExpenseState.ENTER_DESCRIPTION);
                sendMessage(chatId, "📝 Введите описание расхода:");
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, "⚠️ Пожалуйста, введите корректную дату в формате ГГГГ-ММ-ДД:");
            }
        } else if (currentState.getState() == ExpenseState.ENTER_DESCRIPTION) {
            currentState.setDescription(messageText);
            currentState.setState(ExpenseState.ENTER_CATEGORY);
            sendMessage(chatId, "📂 Введите категорию расхода:");
        } else if (currentState.getState() == ExpenseState.ENTER_CATEGORY) {
            currentState.setCategory(messageText);

            sendDeductFromBudgetPrompt(chatId, currentState);
        }
    }

    private void sendDeductFromBudgetPrompt(String chatId, ExpenseCreationState currentState) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        String formattedAmount;
        if (currentState.getAmount() == Math.floor(currentState.getAmount())) {
            formattedAmount = String.format("%.0f", currentState.getAmount());
        } else {
            formattedAmount = String.format("%.2f", currentState.getAmount());
        }

        message.setText("📉 Вычесть сумму " + formattedAmount + " руб. из текущего бюджета?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(BUTTON_CONFIRM);
        yesButton.setCallbackData("DEDUCT_FROM_BUDGET_YES");

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(BUTTON_CANCEL_UPDATE);
        noButton.setCallbackData("DEDUCT_FROM_BUDGET_NO");

        buttons.add(Arrays.asList(yesButton, noButton));
        markup.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        sendMessageForExpense(message);
    }

    private void deductExpenseFromBudget(String chatId, ExpenseCreationState currentState) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "Пользователь не зарегистрирован. Используйте /start для регистрации. 📝");
            return;
        }

        Budget existingBudget = budgetService.findByUserChatId(user.getChatId());
        if (existingBudget != null) {
            existingBudget.setAmount(existingBudget.getAmount() - currentState.getAmount());
            budgetService.save(existingBudget);
        } else {
            sendMessage(chatId, "Бюджет еще не создан. Создайте бюджет, чтобы вычитать расходы. 📉");
        }

        finalizeExpenseCreation(chatId);
    }

    private void finalizeExpenseCreation(String chatId) {
        ExpenseCreationState currentState = expenseCreationStates.get(chatId);

        createExpense(currentState.getTitle(), currentState.getAmount(), currentState.getDate(),
                currentState.getDescription(), currentState.getCategory(), chatId);

        expenseCreationStates.remove(chatId);

        String formattedAmount;
        if (currentState.getAmount() == Math.floor(currentState.getAmount())) {
            formattedAmount = String.format("%.0f", currentState.getAmount());
        } else {
            formattedAmount = String.format("%.2f", currentState.getAmount());
        }

        sendMessage(chatId, "Расход '" + currentState.getTitle() + "' на сумму " + formattedAmount + " руб. создан. 💸");
    }

    private void createExpense(String title, double amount, Timestamp date, String description, String category, String chatId) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "❌ Пользователь не зарегистрирован. Используйте /start для регистрации.");
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
        button1_1.setText(BUTTON_EAT);
        button1_1.setCallbackData("Еда");
        row1.add(button1_1);

        InlineKeyboardButton button1_2 = new InlineKeyboardButton();
        button1_2.setText(BUTTON_TRANSPORT);
        button1_2.setCallbackData("Транспорт");
        row1.add(button1_2);

        rowsInline.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2_1 = new InlineKeyboardButton();
        button2_1.setText(BUTTON_ENTERTAINMENTS);
        button2_1.setCallbackData("Развлечения");
        row2.add(button2_1);

        InlineKeyboardButton button2_2 = new InlineKeyboardButton();
        button2_2.setText(BUTTON_UTILITIES);
        button2_2.setCallbackData("Коммунальные услуги");
        row2.add(button2_2);

        rowsInline.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3_1 = new InlineKeyboardButton();
        button3_1.setText(BUTTON_MEDICINE);
        button3_1.setCallbackData("Медицина");
        row3.add(button3_1);

        InlineKeyboardButton button3_2 = new InlineKeyboardButton();
        button3_2.setText(BUTTON_OTHER);
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
        if (parts.length > 1 && !messageText.equals(BUTTON_UPDATE_INCOME_TEXT)) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте команду /update_income только без параметров.");
            return;
        }

        List<Income> incomes = incomeService.findIncomesByUserId(Long.parseLong(chatId));

        if (incomes.isEmpty()) {
            sendMessage(chatId, "У вас пока нет доходов для обновления. 😕");
            return;
        }

        InlineKeyboardMarkup markup = createIncomesMarkup(incomes);

        SendMessage message = createMessage(chatId, "✨ Выберите доход для обновления:", markup);

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
            button.setText("💰 Запись о доходах №" + income.getId() + ": " + income.getTitle() + " 💼");
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
            sendMessage(chatId, "❌ Ошибка обновления дохода.");
            return;
        }

        Long incomeId = currentState.getIncomeId();

        Income income = incomeService.findById(incomeId);
        if (income == null || !income.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "❌ Доход с указанным номером не найден или не принадлежит вам.");
            return;
        }

        currentState.setOriginalAmount(income.getAmount());

        String currentTitle = income.getTitle();
        Double currentAmount = income.getAmount();
        Timestamp currentDate = income.getDate();
        String currentDescription = income.getDescription();
        String currentCategory = income.getCategory();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = currentDate.toLocalDateTime().format(formatter);

        String selectionMessage = "Выберите, что вы хотите обновить для дохода:\n";
        selectionMessage += "🎯 Текущее название: " + currentTitle + "\n";
        selectionMessage += "💲 Текущая сумма: " + formatNumber(currentAmount) + " руб.\n";
        selectionMessage += "📅 Дата дохода: " + formattedDate + "\n";
        selectionMessage += "📝 Описание: " + currentDescription + "\n";
        selectionMessage += "🏷️ Категория: " + currentCategory + "\n";

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
        row1.add(createInlineButton(BUTTON_TITLE, "update_title_income"));
        row1.add(createInlineButton(BUTTON_SUM, "update_amount_income"));
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton(BUTTON_DATE, "update_date_income"));
        row2.add(createInlineButton(BUTTON_DESCRIPTION, "update_description_income"));
        keyboard.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton(BUTTON_CATEGORY, "update_category_income"));
        row3.add(createInlineButton(BUTTON_CANCEL, "update_cancel_income"));
        keyboard.add(row3);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void processFieldAndValueForIncome(String chatId, String messageText) {
        IncomeUpdateState currentState = incomeUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "❌ Ошибка при обновлении дохода.");
            return;
        }

        Long incomeId = currentState.getIncomeId();
        String fieldToUpdate = currentState.getFieldToUpdate();

        Income income = incomeService.findById(incomeId);
        if (income == null || !income.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "❌ Доход с указанным номером не найден или не принадлежит вам.");
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
                    sendMessage(chatId, "❌ Ошибка при обновлении дохода.");
                    incomeUpdateStates.remove(chatId);
                    return;
            }

            incomeService.save(income);

            sendIncomeUpdateConfirmationMessage(chatId, income, fieldToUpdate);
        } catch (IllegalArgumentException e) {
            sendMessage(chatId, "❌ Ошибка при обработке введенного значения. Попробуйте снова.");
        }
    }

    private void sendIncomeUpdateConfirmationMessage(String chatId, Income income, String updatedField) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder confirmationMessage = new StringBuilder("🎉 Изменения сохранены:\n");
        confirmationMessage.append("📝 Название: ").append(income.getTitle()).append("\n");
        confirmationMessage.append("📝 Описание: ").append(income.getDescription()).append("\n");
        confirmationMessage.append("💲 Сумма: ").append(formatNumber(income.getAmount())).append(" руб.\n");
        confirmationMessage.append("🏷️ Категория: ").append(income.getCategory()).append("\n");
        confirmationMessage.append("\n📅 Дата создания: ").append(income.getDate().toLocalDateTime().format(formatter));

        InlineKeyboardMarkup markup;
        if ("amount".equals(updatedField)) {
            confirmationMessage.append("\n\n📊 Подтвердить изменения и как вы хотите внести изменения в бюджет?");
            markup = createIncomeAmountConfirmationMarkup();
        } else {
            confirmationMessage.append("\n\n📋 Подтвердить изменения?");
            markup = createIncomeConfirmationMarkup();
        }

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createIncomeAmountConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton(BUTTON_CONFIRM, "confirm_update_income"));
        row1.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_update_income"));
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton(BUTTON_CONFIRM_BUDGET_INCOME, "confirm_update_income_add_to_budget"));
        keyboard.add(row2);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private boolean adjustBudgetForIncome(String chatId, double amountDifference) {
        Budget budget = budgetService.findByUserChatId(Long.parseLong(chatId));
        if (budget != null) {
            budget.setAmount(budget.getAmount() + amountDifference);
            budgetService.save(budget);
            return true;
        } else {
            return false;
        }
    }

    private boolean adjustBudgetForExpense(String chatId, double amountDifference) {
        Budget budget = budgetService.findByUserChatId(Long.parseLong(chatId));
        if (budget != null) {
            budget.setAmount(budget.getAmount() - amountDifference);
            budgetService.save(budget);
            return true;
        } else {
            return false;
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
        sendMessage(chatId, "❌ Изменения отменены.");
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
        sendMessage(chatId, "💬 Введите новое значение для поля " + fieldDisplayName + ":");
    }

    public void handleUpdateExpenseCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !messageText.equals(BUTTON_UPDATE_EXPENSE_TEXT)) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте команду /update_expense только без параметров.");
            return;
        }

        List<Expense> expenses = expenseService.findExpensesByUserId(Long.parseLong(chatId));

        if (expenses.isEmpty()) {
            sendMessage(chatId, "📉 У вас пока нет расходов для обновления.");
            return;
        }

        InlineKeyboardMarkup markup = createExpensesMarkup(expenses);

        SendMessage message = createMessage(chatId, "💸 Выберите расход для обновления:", markup);

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
            button.setText("💸 Запись о расходах №" + expense.getId() + ": " + expense.getTitle() + " 💼");
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
            sendMessage(chatId, "❌ Ошибка обновления расхода.");
            return;
        }

        Long expenseId = currentState.getExpenseId();

        Expense expense = expenseService.findById(expenseId);
        if (expense == null || !expense.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "❌ Расход с указанным номером не найден или не принадлежит вам.");
            return;
        }

        currentState.setOriginalAmount(expense.getAmount());

        String currentTitle = expense.getTitle();
        Double currentAmount = expense.getAmount();
        Timestamp currentDate = expense.getDate();
        String currentDescription = expense.getDescription();
        String currentCategory = expense.getCategory();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = currentDate.toLocalDateTime().format(formatter);

        String selectionMessage = "📉 Выберите, что вы хотите обновить для расхода:\n";
        selectionMessage += "📝 Текущее название: " + currentTitle + "\n";
        selectionMessage += "💲 Текущая сумма: " + formatNumber(currentAmount) + " руб.\n";
        selectionMessage += "📅 Дата расхода: " + formattedDate + "\n";
        selectionMessage += "📝 Описание: " + currentDescription + "\n";
        selectionMessage += "🏷️ Категория: " + currentCategory + "\n";

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
        row1.add(createInlineButton(BUTTON_TITLE, "update_title_expense"));
        row1.add(createInlineButton(BUTTON_SUM, "update_amount_expense"));
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton(BUTTON_DATE, "update_date_expense"));
        row2.add(createInlineButton(BUTTON_DESCRIPTION, "update_description_expense"));
        keyboard.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton(BUTTON_CATEGORY, "update_category_expense"));
        row3.add(createInlineButton(BUTTON_CANCEL, "update_cancel_expense"));
        keyboard.add(row3);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    public void processFieldAndValueForExpense(String chatId, String messageText) {
        ExpenseUpdateState currentState = expenseUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "❌ Ошибка при обновлении расхода.");
            return;
        }

        Long expenseId = currentState.getExpenseId();
        String fieldToUpdate = currentState.getFieldToUpdate();

        Expense expense = expenseService.findById(expenseId);
        if (expense == null || !expense.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "❌ Расход с указанным номером не найден или не принадлежит вам.");
            expenseUpdateStates.remove(chatId);
            return;
        }

        try {
            switch (fieldToUpdate) {
                case "title":
                    expense.setTitle(messageText);
                    break;
                case "amount":
                    double newAmount = Double.parseDouble(messageText);
                    currentState.setNewAmount(newAmount);
                    expense.setAmount(newAmount);
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
                    sendMessage(chatId, "❌ Ошибка при обновлении расхода.");
                    expenseUpdateStates.remove(chatId);
                    return;
            }

            expenseService.save(expense);

            sendExpenseUpdateConfirmationMessage(chatId, expense, fieldToUpdate);
        } catch (IllegalArgumentException e) {
            sendMessage(chatId, "❌ Ошибка при обработке введенного значения. Попробуйте снова.");
        }
    }

    private void sendExpenseUpdateConfirmationMessage(String chatId, Expense expense, String updatedField) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder confirmationMessage = new StringBuilder("✅ Изменения сохранены:\n");
        confirmationMessage.append("📝 Название: ").append(expense.getTitle()).append("\n");
        confirmationMessage.append("📝 Описание: ").append(expense.getDescription()).append("\n");
        confirmationMessage.append("💲 Сумма: ").append(formatNumber(expense.getAmount())).append(" руб.\n");
        confirmationMessage.append("🏷️ Категория: ").append(expense.getCategory()).append("\n");
        confirmationMessage.append("\n📅 Дата создания: ").append(expense.getDate().toLocalDateTime().format(formatter));

        InlineKeyboardMarkup markup;
        if ("amount".equals(updatedField)) {
            confirmationMessage.append("\n\nПодтвердить изменения и как вы хотите внести изменения в бюджет?");
            markup = createExpenseAmountConfirmationMarkup();
        } else {
            confirmationMessage.append("\n\nПодтвердить изменения?");
            markup = createExpenseConfirmationMarkup();
        }

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createExpenseAmountConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton(BUTTON_CONFIRM, "confirm_update_expense"));
        row1.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_update_expense"));
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton(BUTTON_CONFIRM_BUDGET_EXPENSE, "confirm_update_expense_change_budget"));
        keyboard.add(row2);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private InlineKeyboardMarkup createExpenseConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow(BUTTON_CONFIRM, "confirm_update_expense");
        row.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_update_expense"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void cancelExpenseUpdate(String chatId, ExpenseUpdateState currentState) {
        expenseService.save(currentState.getOriginalExpense());
        expenseUpdateStates.remove(chatId);
        sendMessage(chatId, "❌ Изменения отменены.");
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
        sendMessage(chatId, "🔧 Введите новое значение для поля " + fieldDisplayName + ":");
    }

    private void handleDeleteIncomeCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals(BUTTON_DELETE_INCOME_TEXT))) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /delete_income без параметров.");
            return;
        }

        List<Income> incomes = incomeService.findIncomesByUserId(Long.parseLong(chatId));

        if (incomes.isEmpty()) {
            sendMessage(chatId, "ℹ️ У вас нет доходов для удаления.");
            return;
        }

        InlineKeyboardMarkup markup = createDeleteIncomeMarkup(incomes);

        SendMessage message = createMessage(chatId, "🗑️ Выберите доход для удаления:", markup);

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
            button.setText("💵 Запись о доходах №" + income.getId() + ": " + income.getTitle());
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
            sendMessage(chatId, "⚠️ Запись о доходе не найдена.");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder confirmationMessage = new StringBuilder("❓ Вы уверены, что хотите удалить следующую запись о доходе?\n\n");
        confirmationMessage.append("📝 Название: ").append(income.getTitle()).append("\n");
        confirmationMessage.append("💲 Сумма: ").append(formatNumber(income.getAmount())).append(" руб.\n");
        confirmationMessage.append("🏷️ Категория: ").append(income.getCategory()).append("\n");
        confirmationMessage.append("📅 Дата: ").append(income.getDate().toLocalDateTime().format(formatter)).append("\n");

        if (income.getDescription() != null && !income.getDescription().isEmpty()) {
            confirmationMessage.append("📝 Описание: ").append(income.getDescription()).append("\n");
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
        sendMessage(chatId, "🗑️ Запись о доходе удалена.");
    }

    private void cancelDeleteIncome(String chatId) {
        incomeDeletionStates.remove(chatId);
        sendMessage(chatId, "❌ Удаление отменено.");
    }

    private void handleDeleteExpenseCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals(BUTTON_DELETE_EXPENSE_TEXT))) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /delete_expense без параметров.");
            return;
        }

        List<Expense> expenses = expenseService.findExpensesByUserId(Long.parseLong(chatId));

        if (expenses.isEmpty()) {
            sendMessage(chatId, "📉 У вас нет расходов для удаления.");
            return;
        }

        InlineKeyboardMarkup markup = createDeleteExpenseMarkup(expenses);

        SendMessage message = createMessage(chatId, "🗑️ Выберите расход для удаления:", markup);

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
            button.setText("💸 Запись о расходах №" + expense.getId() + ": " + expense.getTitle());
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
            sendMessage(chatId, "❌ Запись о расходе не найдена.");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder confirmationMessage = new StringBuilder("⚠️ Вы уверены, что хотите удалить следующую запись о расходе?\n\n");
        confirmationMessage.append("📌 Название: ").append(expense.getTitle()).append("\n");
        confirmationMessage.append("💰 Сумма: ").append(formatNumber(expense.getAmount())).append(" руб.\n");
        confirmationMessage.append("🏷️ Категория: ").append(expense.getCategory()).append("\n");
        confirmationMessage.append("📅 Дата: ").append(expense.getDate().toLocalDateTime().format(formatter)).append("\n");

        if (expense.getDescription() != null && !expense.getDescription().isEmpty()) {
            confirmationMessage.append("📝 Описание: ").append(expense.getDescription()).append("\n");
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
        sendMessage(chatId, "🗑️ Запись о расходе удалена.");
    }

    private void cancelDeleteExpense(String chatId) {
        expenseDeletionStates.remove(chatId);
        sendMessage(chatId, "❌ Удаление отменено.");
    }

    private void handleBudgetCreationCommand(String chatId, String[] parts, String messageText) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "❗️ Пользователь не зарегистрирован. Используйте /start для регистрации.");
            return;
        }

        Budget existingBudget = budgetService.findByUserChatId(user.getChatId());
        if (existingBudget != null) {
            sendMessage(chatId, "📊 У вас уже есть бюджет. Вы не можете создать новый. Для создания нового бюджета удалите существующий.");
            return;
        }

        if (parts.length == 1 || messageText.equals(BUTTON_CREATE_BUDGET_TEXT)) {
            startBudgetCreation(chatId);
        } else {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /add_budget без параметров.");
        }
    }

    private void startBudgetCreation(String chatId) {
        BudgetCreationState currentState = budgetCreationStates.get(chatId);
        if (currentState == null) {
            currentState = new BudgetCreationState();
            budgetCreationStates.put(chatId, currentState);
        }

        currentState.setState(BudgetState.ENTER_NAME);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🌟 Введите название бюджета:");
        sendMessageForBudget(message);
    }

    private void processBudgetCreation(String chatId, String messageText) {
        BudgetCreationState currentState = budgetCreationStates.get(chatId);

        if (currentState.getState() == BudgetState.ENTER_NAME) {
            currentState.setName(messageText);
            currentState.setState(BudgetState.ENTER_AMOUNT);
            sendMessage(chatId, "📝 Введите сумму бюджета (в рублях):");

        } else if (currentState.getState() == BudgetState.ENTER_AMOUNT) {
            try {
                double amount = Double.parseDouble(messageText);
                currentState.setAmount(amount);
                currentState.setState(BudgetState.ENTER_WARNING_THRESHOLD);
                sendMessage(chatId, "💡 Введите сумму, при которой хотите получать предупреждение о малом бюджете (в рублях):");

            } catch (NumberFormatException e) {
                sendMessage(chatId, "❌ Пожалуйста, введите корректное числовое значение для суммы бюджета:");
            }
        } else if (currentState.getState() == BudgetState.ENTER_WARNING_THRESHOLD) {
            try {
                double warningThreshold = Double.parseDouble(messageText);
                currentState.setWarningThreshold(warningThreshold);
                currentState.setState(BudgetState.ENTER_START_DATE);
                sendMessage(chatId, "📅 Введите дату начала бюджета (в формате ГГГГ-ММ-ДД):");

            } catch (NumberFormatException e) {
                sendMessage(chatId, "❌ Пожалуйста, введите корректное числовое значение для порога предупреждения:");
            }
        } else if (currentState.getState() == BudgetState.ENTER_START_DATE) {
            try {
                Timestamp startDate = Timestamp.valueOf(messageText + " 00:00:00");
                currentState.setStartDate(startDate);
                currentState.setState(BudgetState.ENTER_END_DATE);
                sendMessage(chatId, "📅 Введите дату окончания бюджета (в формате ГГГГ-ММ-ДД):");
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, "❌ Пожалуйста, введите корректную дату в формате ГГГГ-ММ-ДД:");
            }
        } else if (currentState.getState() == BudgetState.ENTER_END_DATE) {
            try {
                Timestamp endDate = Timestamp.valueOf(messageText + " 00:00:00");
                if (currentState.getStartDate().after(endDate)) {
                    sendMessage(chatId, "❌ Дата окончания не может предшествовать дате начала. Пожалуйста, введите корректную дату окончания бюджета (в формате ГГГГ-ММ-ДД):");
                } else {
                    currentState.setEndDate(endDate);
                    currentState.setState(BudgetState.ENTER_CATEGORY);
                    sendMessage(chatId, "📊 Введите категорию бюджета:");
                }
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, "❌ Пожалуйста, введите корректную дату в формате ГГГГ-ММ-ДД:");
            }
        } else if (currentState.getState() == BudgetState.ENTER_CATEGORY) {
            currentState.setCategory(messageText);

            String formattedAmount;
            if (currentState.getAmount() == Math.floor(currentState.getAmount())) {
                formattedAmount = String.format("%.0f", currentState.getAmount());
            } else {
                formattedAmount = String.format("%.2f", currentState.getAmount());
            }

            createBudget(currentState.getName(), currentState.getAmount(), currentState.getStartDate(),
                    currentState.getEndDate(), currentState.getCategory(), currentState.getWarningThreshold(), chatId);
            budgetCreationStates.remove(chatId);

            sendMessage(chatId, "🎉 Бюджет '" + currentState.getName() + "' с суммой " + formattedAmount + " руб. создан.");
        }
    }

    private void createBudget(String name, double amount, Timestamp startDate, Timestamp endDate, String category, double warningThreshold, String chatId) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "❌ Пользователь не зарегистрирован. Используйте /start для регистрации.");
            return;
        }

        Budget budget = new Budget();
        budget.setName(name);
        budget.setAmount(amount);
        budget.setInitialAmount(amount);
        budget.setStartDate(startDate);
        budget.setEndDate(endDate);
        budget.setCategory(category);
        budget.setWarningThreshold(warningThreshold);
        budget.setUser(user);

        budgetService.save(budget);
    }

    private void sendMessageForBudget(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void handleUpdateBudgetCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !messageText.equals(BUTTON_UPDATE_BUDGET_TEXT)) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте команду /update_budget только без параметров.");
            return;
        }

        List<Budget> budgets = budgetService.findBudgetsByUserId(Long.parseLong(chatId));

        if (budgets.isEmpty()) {
            sendMessage(chatId, "У вас пока нет бюджета для обновления. 😕");
            return;
        }

        InlineKeyboardMarkup markup = createBudgetsMarkup(budgets);

        SendMessage message = createMessage(chatId, "Выберите бюджет для обновления: 📊", markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending budget selection message: {}", e.getMessage());
        }
    }

    public InlineKeyboardMarkup createBudgetsMarkup(List<Budget> budgets) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Budget budget : budgets) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("📊 Запись о бюджете №" + budget.getId() + ": " + budget.getName());
            button.setCallbackData("update_budget_" + budget.getId());

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);

            keyboard.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    public void sendFieldSelectionMessageForBudget(String chatId) {
        BudgetUpdateState currentState = budgetUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "❌ Ошибка обновления бюджета. Пожалуйста, попробуйте снова.");
            return;
        }

        Long budgetId = currentState.getBudgetId();

        Budget budget = budgetService.findById(budgetId);
        if (budget == null || !budget.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "❗ Бюджет с указанным номером не найден или не принадлежит вам.");
            return;
        }

        String currentName = budget.getName();
        Double currentAmount = budget.getAmount();

        LocalDate startDate = budget.getStartDate().toLocalDateTime().toLocalDate();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedStartDate = startDate.format(dateFormatter);

        LocalDate endDate = budget.getEndDate().toLocalDateTime().toLocalDate();
        String formattedEndDate = endDate.format(dateFormatter);

        String currentCategory = budget.getCategory();
        Double currentWarningThreshold = budget.getWarningThreshold();

        String selectionMessage = "🔍 Выберите, что вы хотите обновить для бюджета:\n\n";
        selectionMessage += "📛 Текущее название: " + currentName + "\n";
        selectionMessage += "💰 Текущая сумма: " + formatNumber(currentAmount) + " руб.\n";
        selectionMessage += "📅 Дата начала: " + formattedStartDate + "\n";
        selectionMessage += "📅 Дата окончания: " + formattedEndDate + "\n";
        selectionMessage += "📂 Категория: " + currentCategory + "\n";
        selectionMessage += "⚠️ Текущий порог предупреждения: " + formatNumber(currentWarningThreshold) + " руб.\n";

        InlineKeyboardMarkup markup = createUpdateMarkupForBudget();

        SendMessage message = createMessage(chatId, selectionMessage, markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending field selection message for budget: {}", e.getMessage());
        }
    }

    public InlineKeyboardMarkup createUpdateMarkupForBudget() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton(BUTTON_TITLE, "update_name_budget"));
        row1.add(createInlineButton(BUTTON_SUM, "update_amount_budget"));
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton(BUTTON_START_DATE, "update_start_date_budget"));
        row2.add(createInlineButton(BUTTON_END_DATE, "update_end_date_budget"));
        keyboard.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton(BUTTON_CATEGORY, "update_category_budget"));
        row3.add(createInlineButton(BUTTON_WARNING, "update_warning_threshold_budget"));
        keyboard.add(row3);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(createInlineButton(BUTTON_CANCEL, "update_cancel_budget"));
        keyboard.add(row4);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    public void processFieldAndValueForBudget(String chatId, String messageText) {
        BudgetUpdateState currentState = budgetUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "❌ Ошибка при обновлении бюджета.");
            return;
        }

        Long budgetId = currentState.getBudgetId();
        String fieldToUpdate = currentState.getFieldToUpdate();

        Budget budget = budgetService.findById(budgetId);
        if (budget == null || !budget.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "❗ Бюджет с указанным номером не найден или не принадлежит вам.");
            budgetUpdateStates.remove(chatId);
            return;
        }

        try {
            switch (fieldToUpdate) {
                case "name":
                    budget.setName(messageText);
                    break;
                case "amount":
                    double amount = Double.parseDouble(messageText);
                    budget.setAmount(amount);
                    break;
                case "start_date":
                    Timestamp startDate = Timestamp.valueOf(messageText + " 00:00:00");
                    budget.setStartDate(startDate);
                    break;
                case "end_date":
                    Timestamp endDate = Timestamp.valueOf(messageText + " 00:00:00");
                    budget.setEndDate(endDate);
                    break;
                case "category":
                    budget.setCategory(messageText);
                    break;
                case "warning_threshold":
                    double warningThreshold = Double.parseDouble(messageText);
                    budget.setWarningThreshold(warningThreshold);
                    break;
                default:
                    sendMessage(chatId, "❌ Ошибка при обновлении бюджета.");
                    budgetUpdateStates.remove(chatId);
                    return;
            }

            budgetService.save(budget);

            sendBudgetUpdateConfirmationMessage(chatId, budget);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "⚠️ Ошибка при обработке введенного значения. Пожалуйста, введите корректное числовое значение.");
        } catch (IllegalArgumentException e) {
            sendMessage(chatId, "⚠️ Ошибка при обработке введенного значения. Пожалуйста, введите корректное значение в правильном формате.");
        }
    }

    public void sendBudgetUpdateConfirmationMessage(String chatId, Budget budget) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder confirmationMessage = new StringBuilder("✅ Изменения сохранены:\n\n");
        confirmationMessage.append("\uD83D\uDCDB Название: ").append(budget.getName()).append("\n");
        confirmationMessage.append("💰 Сумма: ").append(formatNumber(budget.getAmount())).append(" руб.\n");
        confirmationMessage.append("📅 Дата начала: ").append(budget.getStartDate().toLocalDateTime().format(formatter)).append("\n");
        confirmationMessage.append("📅 Дата окончания: ").append(budget.getEndDate().toLocalDateTime().format(formatter)).append("\n");
        confirmationMessage.append("\uD83D\uDCC2 Категория: ").append(budget.getCategory()).append("\n");
        confirmationMessage.append("⚠️ Текущий порог предупреждения: ").append(formatNumber(budget.getWarningThreshold())).append(" руб.\n");

        confirmationMessage.append("\n\nПодтвердить изменения?");

        InlineKeyboardMarkup markup = createBudgetConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createBudgetConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow(BUTTON_CONFIRM, "confirm_update_budget");
        row.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_update_budget"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void cancelBudgetUpdate(String chatId, BudgetUpdateState currentState) {
        budgetService.save(currentState.getOriginalBudget());
        budgetUpdateStates.remove(chatId);
        sendMessage(chatId, "❌ Изменения отменены.");
    }

    private void sendNewValueRequestForBudget(String chatId, String field) {
        String fieldDisplayName = switch (field) {
            case "name" -> "название";
            case "amount" -> "сумма";
            case "start_date" -> "дата начала";
            case "end_date" -> "дата окончания";
            case "category" -> "категория";
            case "warning_threshold" -> "предупреждение о малом бюджете";
            default -> "";
        };
        sendMessage(chatId, "✏️ Введите новое значение для поля " + fieldDisplayName + ":");
    }

    private void handleDeleteBudgetCommand(String[] parts, String chatId, String messageText) {
        if (parts.length > 1 && !(messageText.equals(BUTTON_DELETE_BUDGET_TEXT))) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /delete_budget без параметров.");
            return;
        }

        List<Budget> budgets = budgetService.findBudgetsByUserId(Long.parseLong(chatId));

        if (budgets.isEmpty()) {
            sendMessage(chatId, "🔍 У вас нет бюджета для удаления.");
            return;
        }

        InlineKeyboardMarkup markup = createDeleteBudgetMarkup(budgets);

        SendMessage message = createMessage(chatId, "🗑️ Выберите бюджет для удаления:", markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending delete budget selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createDeleteBudgetMarkup(List<Budget> budgets) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Budget budget : budgets) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("💼 " + budget.getName());
            button.setCallbackData("delete_budget_" + budget.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void sendDeleteBudgetConfirmationMessage(String chatId, Long budgetIdToDelete) {
        Budget budget = budgetService.findById(budgetIdToDelete);
        if (budget == null) {
            sendMessage(chatId, "🚫 Запись о бюджете не найдена.");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder confirmationMessage = new StringBuilder("❓ Вы уверены, что хотите удалить следующий бюджет?\n\n");
        confirmationMessage.append("📛 Название: ").append(budget.getName()).append("\n");
        confirmationMessage.append("💵 Начальная сумма: ").append(formatNumber(budget.getInitialAmount())).append(" руб.\n");
        confirmationMessage.append("💰 Текущая сумма: ").append(formatNumber(budget.getAmount())).append(" руб.\n");
        confirmationMessage.append("📂 Категория: ").append(budget.getCategory()).append("\n");
        confirmationMessage.append("📅 Начальная дата: ").append(budget.getStartDate().toLocalDateTime().format(formatter)).append("\n");
        confirmationMessage.append("📅 Конечная дата: ").append(budget.getEndDate().toLocalDateTime().format(formatter)).append("\n");
        confirmationMessage.append("⚠️ Порог предупреждения: ").append(formatNumber(budget.getWarningThreshold())).append(" руб.\n");

        InlineKeyboardMarkup markup = createDeleteBudgetConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending delete confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createDeleteBudgetConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow(BUTTON_CONFIRM, "confirm_delete_budget");
        row.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_delete_budget"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void confirmDeleteBudget(String chatId, Long budgetIdToDelete) {
        budgetService.deleteById(budgetIdToDelete);
        budgetDeletionStates.remove(chatId);
        sendMessage(chatId, "🗑️ Запись о бюджете удалена.");
    }

    private void cancelDeleteBudget(String chatId) {
        budgetDeletionStates.remove(chatId);
        sendMessage(chatId, "❌ Удаление отменено.");
    }

    private String formatNumber(Number number) {
        String formattedNumber;
        if (number == null) {
            formattedNumber = "0";
        } else if (number.doubleValue() == Math.floor(number.doubleValue())) {
            formattedNumber = String.format("%.0f", number.doubleValue());
        } else {
            formattedNumber = String.format(Locale.US, "%.2f", number.doubleValue());
        }
        return formattedNumber;
    }

    private void sendCurrentBudgetInfo(String chatId, String[] parts, String messageText) {
        if (parts.length == 1 || messageText.equals(BUTTON_INFO_ABOUT_BUDGET_TEXT)) {
            String budgetInfo = getCurrentBudgetInfo(chatId);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(budgetInfo);

            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /current_budget без параметров.");
        }
    }

    public String getCurrentBudgetInfo(String chatId) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            return "🚫 Пользователь не зарегистрирован. Используйте /start для регистрации.";
        }

        Budget currentBudget = budgetService.findByUserChatId(user.getChatId());
        if (currentBudget == null) {
            return "ℹ️ У вас нет текущего бюджета.";
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder sb = new StringBuilder();
        sb.append("💼 Информация о текущем бюджете:\n\n");
        sb.append("📛 Название: ").append(currentBudget.getName()).append("\n");
        sb.append("💰 Начальная сумма: ").append(formatNumber(currentBudget.getInitialAmount())).append(" руб.\n");
        sb.append("💸 Текущая сумма: ").append(formatNumber(currentBudget.getAmount())).append(" руб.\n");
        sb.append("📂 Категория: ").append(currentBudget.getCategory()).append("\n");
        sb.append("📅 Начало: ").append(currentBudget.getStartDate().toLocalDateTime().format(dateFormatter)).append("\n");
        sb.append("🏁 Окончание: ").append(currentBudget.getEndDate().toLocalDateTime().format(dateFormatter)).append("\n");
        sb.append("⚠️ Порог предупреждения: ").append(formatNumber(currentBudget.getWarningThreshold())).append(" руб.\n");

        return sb.toString();
    }

    private void handleReportCreationCommand(String chatId, String[] parts, String messageText) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "❌ Пользователь не зарегистрирован. Используйте /start для регистрации.");
            return;
        }

        if (parts.length == 1 || messageText.equals(BUTTON_SHOW_ANALYTIC_TEXT)) {
            startReportCreation(chatId);
        } else {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте /add_report без параметров.");
        }
    }

    private void startReportCreation(String chatId) {
        ReportCreationState currentState = reportCreationStates.get(chatId);
        if (currentState == null) {
            currentState = new ReportCreationState();
            reportCreationStates.put(chatId, currentState);
        }

        currentState.setState(ReportState.ENTER_START_DATE);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("✨ Введите дату начала периода отчета (в формате ГГГГ-ММ-ДД):");
        sendMessageForReport(message);
    }

    private void processReportCreation(String chatId, String messageText) {
        ReportCreationState currentState = reportCreationStates.get(chatId);

        if (currentState.getState() == ReportState.ENTER_START_DATE) {
            try {
                Timestamp startDate = Timestamp.valueOf(messageText + " 00:00:00");
                currentState.setStartDate(startDate);
                currentState.setState(ReportState.ENTER_END_DATE);
                sendMessage(chatId, "✨ Введите дату окончания периода отчета (в формате ГГГГ-ММ-ДД):");
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, "❌ Пожалуйста, введите корректную дату в формате ГГГГ-ММ-ДД:");
            }
        } else if (currentState.getState() == ReportState.ENTER_END_DATE) {
            try {
                Timestamp endDate = Timestamp.valueOf(messageText + " 00:00:00");
                if (currentState.getStartDate().after(endDate)) {
                    sendMessage(chatId, "❌ Дата окончания не может предшествовать дате начала. Пожалуйста, введите корректную дату окончания периода отчета (в формате ГГГГ-ММ-ДД):");
                } else {
                    currentState.setEndDate(endDate);
                    generateReport(chatId, currentState);
                    reportCreationStates.remove(chatId);
                }
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, "❌ Пожалуйста, введите корректную дату в формате ГГГГ-ММ-ДД:");
            }
        }
    }

    private void generateReport(String chatId, ReportCreationState currentState) {
        User user = userService.findByChatId(Long.parseLong(chatId));

        Timestamp startDate = currentState.getStartDate();
        Timestamp endDate = currentState.getEndDate();

        List<Expense> expenses = expenseService.findExpensesByUserAndDateBetween(user, startDate, endDate);
        List<Income> incomes = incomeService.findIncomesByUserAndDateBetween(user, startDate, endDate);

        List<Budget> budgets = budgetService.findBudgetsByUser(user);

        try {
            byte[] pdfBytes = generatePdfReport(user, startDate, endDate, expenses, incomes, budgets);
            sendPdfReport(chatId, pdfBytes);
        } catch (Exception e) {
            sendMessage(chatId, "❌ Ошибка при генерации отчета: " + e.getMessage());
        }
    }

    private byte[] generatePdfReport(User user, Timestamp startDate, Timestamp endDate, List<Expense> expenses, List<Income> incomes, List<Budget> budgets) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        PdfFont font = PdfFontFactory.createFont("fonts/FreeSans.ttf", "CP1251", true);

        String creationDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(System.currentTimeMillis()));

        document.add(new Paragraph("Отчет за период " + formatTimestamp(startDate) + " - " + formatTimestamp(endDate)).setFontSize(14).setBold().setFont(font));
        document.add(new Paragraph("Пользователь: " + user.getFirstName()).setFontSize(12).setFont(font));
        document.add(new Paragraph("Дата создания: " + creationDate).setFontSize(10).setItalic().setFont(font));

        JFreeChart expenseChart = createPieChart("Расходы", expenses.stream().collect(Collectors.toMap(Expense::getTitle, Expense::getAmount, Double::sum)));
        Image expenseChartImage = convertChartToImage(expenseChart);
        document.add(expenseChartImage);
        addAnalyticsText(document, "Расходы", expenses.stream().collect(Collectors.toMap(Expense::getTitle, Expense::getAmount, Double::sum)), font);

        document.add(new AreaBreak());

        JFreeChart incomeChart = createPieChart("Доходы", incomes.stream().collect(Collectors.toMap(Income::getTitle, Income::getAmount, Double::sum)));
        Image incomeChartImage = convertChartToImage(incomeChart);
        document.add(incomeChartImage);
        addAnalyticsText(document, "Доходы", incomes.stream().collect(Collectors.toMap(Income::getTitle, Income::getAmount, Double::sum)), font);

        document.add(new AreaBreak());

        Map<String, Double> budgetData = budgets.stream().collect(Collectors.toMap(Budget::getName, Budget::getAmount, Double::sum));
        Map<String, Double> initialBudgetData = budgets.stream().collect(Collectors.toMap(Budget::getName, Budget::getInitialAmount, Double::sum));
        JFreeChart budgetChart = createBudgetPieChart("Бюджет", budgetData, initialBudgetData);
        Image budgetChartImage = convertChartToImage(budgetChart);
        document.add(budgetChartImage);
        addBudgetAnalyticsText(document, budgets, font);

        document.close();
        return baos.toByteArray();
    }

    private void addBudgetAnalyticsText(Document document, List<Budget> budgets, PdfFont font) {
        if (budgets.isEmpty()) {
            document.add(new Paragraph("Нет данных для отображения аналитики по бюджету").setFontSize(10).setFont(font));
            return;
        }

        StringBuilder analyticsText = new StringBuilder();
        analyticsText.append("Аналитика по бюджету:\n\n");
        for (Budget budget : budgets) {
            double usedAmount = budget.getInitialAmount() - budget.getAmount();
            double usedPercentage = (usedAmount / budget.getInitialAmount()) * 100;
            double remainingPercentage = 100 - usedPercentage;
            analyticsText.append(String.format("Бюджет '%s':\n", budget.getName()));
            analyticsText.append(String.format("- Изначальная сумма: %.2f\n", budget.getInitialAmount()));
            analyticsText.append(String.format("- Потрачено: %.2f (%.2f%%)\n", usedAmount, usedPercentage));
            analyticsText.append(String.format("- Осталось: %.2f (%.2f%%)\n", budget.getAmount(), remainingPercentage));
            analyticsText.append("\n");
        }

        document.add(new Paragraph(analyticsText.toString()).setFontSize(10).setFont(font));
    }

    private JFreeChart createBudgetPieChart(String title, Map<String, Double> budgetData, Map<String, Double> initialBudgetData) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Double> entry : budgetData.entrySet()) {
            double initialAmount = initialBudgetData.get(entry.getKey());
            double remainingAmount = entry.getValue();
            double usedAmount = initialAmount - remainingAmount;

            dataset.setValue(entry.getKey() + " (Потрачено)", usedAmount);
            dataset.setValue(entry.getKey() + " (Осталось)", remainingAmount);
        }
        JFreeChart chart = ChartFactory.createPieChart(title, dataset, true, true, false);
        PiePlot plot = (PiePlot) chart.getPlot();

        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.setNoDataMessage("Нет данных для отображения");
        plot.setCircular(true);
        plot.setLabelGap(0.02);

        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}", new DecimalFormat("0.00%"), new DecimalFormat("0.00%")));

        return chart;
    }

    private void addAnalyticsText(Document document, String title, Map<String, Double> data, PdfFont font) {
        if (data.isEmpty()) {
            document.add(new Paragraph("Нет данных для отображения аналитики по " + title).setFontSize(10).setFont(font));
            return;
        }

        List<Map.Entry<String, Double>> topEntries = data.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
        StringBuilder analyticsText = new StringBuilder();
        analyticsText.append(String.format("Общая сумма в категории '%s': %.2f%n", title, total));
        analyticsText.append(String.format("Общее количество элементов: %d%n", data.size()));
        analyticsText.append("Топ элементы:\n");

        for (Map.Entry<String, Double> entry : topEntries) {
            double percentage = (entry.getValue() / total) * 100;
            analyticsText.append(String.format("- %s: сумма: %.2f, процент: %.2f%%%n", entry.getKey(), entry.getValue(), percentage));
        }

        analyticsText.append("\n");

        document.add(new Paragraph(analyticsText.toString()).setFontSize(10).setFont(font));
    }


    private JFreeChart createPieChart(String title, Map<String, Double> data) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }
        JFreeChart chart = ChartFactory.createPieChart(title, dataset, true, true, false);
        PiePlot plot = (PiePlot) chart.getPlot();

        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.setNoDataMessage("Нет данных для отображения");
        plot.setCircular(true);
        plot.setLabelGap(0.02);

        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}", new DecimalFormat("0.00%"), new DecimalFormat("0.00%")));

        return chart;
    }

    private Image convertChartToImage(JFreeChart chart) throws Exception {
        BufferedImage bufferedImage = chart.createBufferedImage(500, 300);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeBufferedImageAsPNG(baos, bufferedImage);
        return new Image(ImageDataFactory.create(baos.toByteArray()));
    }

    private void sendPdfReport(String chatId, byte[] pdfBytes) throws TelegramApiException {
        String uniqueFileName = "report_" + UUID.randomUUID() + ".pdf";
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(new InputFile(new ByteArrayInputStream(pdfBytes), uniqueFileName));
        execute(sendDocument);
    }

    private void sendMessageForReport(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd").format(timestamp);
    }

    public void prepareAndSendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    private void executeMessage(SendMessage message){
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void handleListIncomesCommand(String chatId, String[] parts, String messageText) {
        if (parts.length > 1 && !messageText.equals(BUTTON_LIST_INCOMES_TEXT)) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте команду /list_incomes только без параметров.");
            return;
        }

        List<Income> incomes = incomeService.findIncomesByUserChatId(Long.parseLong(chatId));
        if (incomes.isEmpty()) {
            sendMessage(chatId, EmojiParser.parseToUnicode(":information_source: У вас нет доходов."));
            return;
        }

        incomes.sort(Comparator.comparing(Income::getDate).reversed());

        StringBuilder messageBuilder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Income income : incomes) {
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCB0 Название: ")).append(income.getTitle()).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCB5 Сумма: ")).append(formatNumber(income.getAmount())).append(" руб.\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCC5 Дата: ")).append(income.getDate().toLocalDateTime().format(formatter)).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCDD Описание: ")).append(income.getDescription() != null ? income.getDescription() : "Без описания").append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCC4 Категория: ")).append(income.getCategory()).append("\n");
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

    private void handleListExpensesCommand(String chatId, String[] parts, String messageText) {
        if (parts.length > 1 && !messageText.equals(BUTTON_LIST_EXPENSES_TEXT)) {
            sendMessage(chatId, "❌ Неверный формат команды. Используйте команду /list_expenses только без параметров.");
            return;
        }

        List<Expense> expenses = expenseService.findExpensesByUserChatId(Long.parseLong(chatId));
        if (expenses.isEmpty()) {
            sendMessage(chatId, EmojiParser.parseToUnicode(":information_source: У вас нет расходов."));
            return;
        }

        expenses.sort(Comparator.comparing(Expense::getDate).reversed());

        StringBuilder messageBuilder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Expense expense : expenses) {
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCB8 Название: ")).append(expense.getTitle()).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCB5 Сумма: ")).append(formatNumber(expense.getAmount())).append(" руб.\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCC5 Дата: ")).append(expense.getDate().toLocalDateTime().format(formatter)).append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCDD Описание: ")).append(expense.getDescription() != null ? expense.getDescription() : "Без описания").append("\n");
            messageBuilder.append(EmojiParser.parseToUnicode("\uD83D\uDCC4 Категория: ")).append(expense.getCategory()).append("\n");
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

}

