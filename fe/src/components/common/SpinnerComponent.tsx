import Image from "next/image";

export default function SpinnerComponent() {
  return (
    <div className="wf flex ac jc">
      <Image src={"/spinner.gif"} alt="spinner" width={1} height={1} />
    </div>
  );
}
