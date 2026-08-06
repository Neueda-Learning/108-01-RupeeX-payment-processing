import BotChat from '../../components/bot-chat';

export const metadata = { title: 'RupeeX Bot' };

export default function Page() {
  return (
    <div className="mx-auto max-w-7xl space-y-8 px-6 py-10 lg:px-8">
      <header className="space-y-2">
        <p className="text-sm font-semibold uppercase tracking-wide text-orange-700">
          AI Assistant
        </p>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">
          Manage payments with natural language
        </h1>
        <p className="text-slate-600">
          Ask the assistant to create or manage payments, check account balances,
          or look up payment status — every action is shown to you for review
          before anything is submitted.
        </p>
      </header>

      <BotChat variant="fullscreen" />
    </div>
  );
}
