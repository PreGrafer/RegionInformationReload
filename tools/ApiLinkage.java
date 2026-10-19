import net.bytebuddy.jar.asm.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Checks the baseline bytecode's direct Bukkit symbols against an alternative API classpath.
 * This does not boot a server or verify reflective optional APIs. */
public final class ApiLinkage {
    private static final Set<String> checked=new HashSet<>();
    private static final List<String> errors=new ArrayList<>();
    private static Class<?> load(String name) throws Exception { return Class.forName(name.replace('/','.'),false,ApiLinkage.class.getClassLoader()); }
    private static Executable method(Class<?> owner,String name,String descriptor,Set<Class<?>> seen) {
        if(owner==null || !seen.add(owner)) return null;
        if(name.equals("<init>")) {
            for(Constructor<?> c:owner.getDeclaredConstructors()) if(net.bytebuddy.jar.asm.Type.getConstructorDescriptor(c).equals(descriptor)) return c;
            return null;
        }
        for(Method m:owner.getDeclaredMethods()) if(m.getName().equals(name) && net.bytebuddy.jar.asm.Type.getMethodDescriptor(m).equals(descriptor)) return m;
        for(Class<?> i:owner.getInterfaces()) { Executable m=method(i,name,descriptor,seen); if(m!=null) return m; }
        return method(owner.getSuperclass(),name,descriptor,seen);
    }
    public static void main(String[] args) throws Exception {
        try(java.util.stream.Stream<Path> files=Files.walk(Paths.get(args[0]))) {
            files.filter(p -> p.toString().endsWith(".class")).forEach(path -> {
                try {
                    new ClassReader(Files.readAllBytes(path)).accept(new ClassVisitor(Opcodes.ASM9) {
                        public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions) {
                            return new MethodVisitor(Opcodes.ASM9) {
                                public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean isInterface) {
                                    String key=owner+"."+method+descriptor;
                                    if(!owner.startsWith("org/bukkit/") || !checked.add(key)) return;
                                    try {
                                        Class<?> type=load(owner); Executable found=ApiLinkage.method(type,method,descriptor,new HashSet<>());
                                        if(found==null) throw new NoSuchMethodException(key);
                                        if((opcode==Opcodes.INVOKESTATIC)!=Modifier.isStatic(found.getModifiers()) && !method.equals("<init>")) throw new IllegalStateException("static mismatch");
                                        if(isInterface!=type.isInterface()) throw new IllegalStateException("interface/class mismatch");
                                    } catch(Throwable e) { errors.add(key+" -> "+e); }
                                }
                                public void visitFieldInsn(int opcode,String owner,String field,String descriptor) {
                                    String key=owner+"."+field+":"+descriptor;
                                    if(!owner.startsWith("org/bukkit/") || !checked.add(key)) return;
                                    try {
                                        Field found=load(owner).getField(field);
                                        if(!net.bytebuddy.jar.asm.Type.getDescriptor(found.getType()).equals(descriptor)) throw new IllegalStateException("field type mismatch");
                                    } catch(Throwable e) { errors.add(key+" -> "+e); }
                                }
                            };
                        }
                    },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
                } catch(Exception e) { throw new RuntimeException(e); }
            });
        }
        if(!errors.isEmpty()) { for(String error:errors) System.err.println(error); throw new AssertionError(errors.size()+" unresolved Bukkit symbols"); }
        System.out.println("Verified "+checked.size()+" direct Bukkit symbols in the baseline bytecode.");
    }
}
