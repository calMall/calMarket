import Image from "next/image";

export default function ContainImage({
  url,
  alt,
}: {
  url: string;
  alt: string;
}) {
  return (
    <>
      <Image
        src={url}
        alt={alt}
        style={{ objectFit: "contain" }}
        priority
        fill
        sizes="100%"
      />
    </>
  );
}
