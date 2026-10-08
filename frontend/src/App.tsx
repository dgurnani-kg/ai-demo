import CataloguePage from "./pages/CataloguePage";

export default function App() {
  return (
    <div className="shell">
      <header>
        <h1>Library</h1>
        <p>Spring Boot · React · one contract</p>
      </header>
      <main>
        <CataloguePage />
      </main>
    </div>
  );
}
