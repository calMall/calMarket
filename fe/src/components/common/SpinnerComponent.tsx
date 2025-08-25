import Image from "next/image";
import ContainImage from "./ContainImage";

export default function SpinnerComponent() {
  return (
    <div className="wf flex ac jc">
      <div className="loading-spinner rt">
        <ContainImage url={"/spinner.gif"} alt="spinner" />
      </div>
    </div>
  );
}
