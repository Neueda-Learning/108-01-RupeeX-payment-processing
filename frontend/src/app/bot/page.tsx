import BotChat from '../../components/bot-chat';

export const metadata = { title: 'RupeeX Bot' };

export default function Page() {
  return (
    <main className="p-6">
      <h1 className="text-2xl font-bold mb-4">RupeeX Assistant</h1>
      <BotChat />
    </main>
  );
}
